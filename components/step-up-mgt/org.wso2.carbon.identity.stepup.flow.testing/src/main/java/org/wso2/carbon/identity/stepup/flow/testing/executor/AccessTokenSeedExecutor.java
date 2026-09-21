/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.stepup.flow.testing.executor;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.IdentityKeyStoreResolver;
import org.wso2.carbon.identity.core.util.IdentityKeyStoreResolverConstants.InboundProtocol;
import org.wso2.carbon.identity.core.util.IdentityKeyStoreResolverException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.stepup.flow.testing.internal.StepUpFlowTestingDataHolder;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_USER_INPUT_REQUIRED;

/**
 * Seeds a secured self-service operations flow from an ordinary OAuth access token.
 * <p>
 * This is a proof of concept stand-in for the real seeding step, which consumes a purpose built launch token. An
 * access token is close enough to demonstrate the part that matters here, namely that the subject and any prior
 * authentication evidence come out of a token rather than being asserted by the caller, but it is not the same
 * thing and must not be mistaken for it:
 * <ul>
 *     <li>it is not single use, so it can be replayed for the life of the token;</li>
 *     <li>it is not restricted to the self-care application, so any token for any application is accepted;</li>
 *     <li>it carries no operation scope, so what the flow may change is taken from the request instead;</li>
 *     <li>it ordinarily carries no {@code acr} and no {@code auth_time}, so there is usually no prior evidence to
 *     inherit and every configured factor comes out unsatisfied.</li>
 * </ul>
 * Only a JWT access token can be read here. An opaque token carries nothing to parse and is rejected.
 * <p>
 * This executor is kept out of the step-up feature and refuses to run unless
 * {@code StepUp.AllowUnverifiedSeed} has been turned on.
 */
public class AccessTokenSeedExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(AccessTokenSeedExecutor.class);

    private static final String EXECUTOR_NAME = "AccessTokenSeedExecutor";
    private static final String ENABLED_PROPERTY = "StepUp.AllowUnverifiedSeed";

    private static final String INPUT_LAUNCH_TOKEN = "launchToken";
    private static final String INPUT_ACCESS_TOKEN = "accessToken";
    private static final String INPUT_OPERATION_TYPE = "operationType";
    private static final String INPUT_OPERATION_ATTRIBUTES = "operationAttributes";
    private static final String INPUT_CALLBACK_URL = "callbackUrl";

    private static final String CLAIM_ACR = "acr";
    private static final String CLAIM_AMR = "amr";
    private static final String CLAIM_AUTH_TIME = "auth_time";
    private static final String CLAIM_CLIENT_ID = "client_id";

    private static final String USERNAME_CLAIM = "http://wso2.org/claims/username";
    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String MOBILE_CLAIM = "http://wso2.org/claims/mobile";

    private static final long CLOCK_SKEW_SECONDS = 300L;

    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        if (!isEnabled()) {
            LOG.error("Refusing to seed a flow from a bearer token. Start the server with -D" + ENABLED_PROPERTY
                    + "=true to allow this on a development server.");
            return error("Token seeding is not enabled on this server.");
        }

        // The token arrives with the request that opened the flow, the way a launch token does. A pasted value is
        // still accepted so the flow can be driven by hand when there is no application to launch it.
        Map<String, String> inputs = context.getUserInputData();
        String accessToken = inputs == null ? null : inputs.get(INPUT_LAUNCH_TOKEN);
        if (StringUtils.isBlank(accessToken) && inputs != null) {
            accessToken = inputs.get(INPUT_ACCESS_TOKEN);
        }
        if (StringUtils.isBlank(accessToken)) {
            ExecutorResponse response = new ExecutorResponse(STATUS_USER_INPUT_REQUIRED);
            response.setRequiredData(Collections.singletonList(INPUT_ACCESS_TOKEN));
            return response;
        }

        JWTClaimsSet claims;
        try {
            claims = readToken(accessToken.trim(), context.getTenantDomain());
        } catch (ParseException e) {
            LOG.error("The supplied access token is not a JWT. An opaque token carries nothing to read here.", e);
            return error("The access token could not be read. Only a JWT access token can seed this flow.");
        } catch (SeedValidationException e) {
            LOG.error("Refusing the supplied access token: " + e.getMessage());
            return error(e.getMessage());
        }

        String subject = claims.getSubject();
        if (StringUtils.isBlank(subject)) {
            return error("The access token carries no subject.");
        }

        try {
            if (!resolveUser(context, subject)) {
                return error("No user found for the subject of the access token.");
            }
        } catch (UserStoreException e) {
            LOG.error("Error while resolving the subject of the access token.", e);
            return error("Error while resolving the subject of the access token.");
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("stepUp.operationType", defaulted(inputs.get(INPUT_OPERATION_TYPE), "UPDATE_PROFILE"));
        properties.put("stepUp.operationAttributes", defaulted(inputs.get(INPUT_OPERATION_ATTRIBUTES), EMAIL_CLAIM));
        properties.put("stepUp.validatedCallbackUrl",
                defaulted(inputs.get(INPUT_CALLBACK_URL), "https://localhost:9443/myaccount"));
        carryPriorEvidence(claims, properties);

        LOG.info("Seeded flow " + context.getContextIdentifier() + " from an access token issued to client: "
                + claims.getClaim(CLAIM_CLIENT_ID));

        ExecutorResponse response = new ExecutorResponse(STATUS_COMPLETE);
        response.setContextProperty(properties);
        return response;
    }

    /**
     * Copies whatever evidence of prior authentication the token actually carries.
     * <p>
     * An ordinary access token normally carries none of this, and that is the point worth observing: with nothing
     * to inherit, every factor the policy requires comes out unsatisfied and must be performed in this ceremony.
     * Nothing is substituted when a claim is missing, because a guess here is indistinguishable from evidence.
     */
    private void carryPriorEvidence(JWTClaimsSet claims, Map<String, Object> properties) {

        Object acr = claims.getClaim(CLAIM_ACR);
        if (acr != null && StringUtils.isNotBlank(acr.toString())) {
            properties.put("stepUp.inheritedAcr", acr.toString());
        } else {
            LOG.info("The access token carries no acr claim. There is no prior assurance level to inherit.");
        }

        List<String> amr = readAmr(claims);
        if (!amr.isEmpty()) {
            properties.put("stepUp.inheritedFactors", String.join(",", amr));
        } else {
            LOG.info("The access token carries no amr claim. There are no prior factors to inherit.");
        }

        Object authTime = claims.getClaim(CLAIM_AUTH_TIME);
        if (authTime instanceof Number) {
            properties.put("stepUp.inheritedAuthTime",
                    Instant.ofEpochSecond(((Number) authTime).longValue()).toString());
        } else {
            LOG.info("The access token carries no auth_time claim. There is no known authentication event, so no "
                    + "freshness requirement can be satisfied by it.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readAmr(JWTClaimsSet claims) {

        Object amr = claims.getClaim(CLAIM_AMR);
        if (amr instanceof List) {
            return (List<String>) ((List<?>) amr).stream()
                    .filter(java.util.Objects::nonNull)
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (amr != null && StringUtils.isNotBlank(amr.toString())) {
            return Collections.singletonList(amr.toString());
        }
        return Collections.emptyList();
    }

    /**
     * Parses the token, verifies it was signed by this server for OAuth, and checks that it is still valid.
     */
    private JWTClaimsSet readToken(String accessToken, String tenantDomain)
            throws ParseException, SeedValidationException {

        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        try {
            RSAPublicKey publicKey = IdentityKeyStoreResolver.getInstance()
                    .getPublicKey(tenantDomain, InboundProtocol.OAUTH);
            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                throw new SeedValidationException("The access token signature is not valid.");
            }
        } catch (IdentityKeyStoreResolverException | JOSEException e) {
            throw new SeedValidationException("The access token signature could not be verified.");
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        Date expiry = claims.getExpirationTime();
        if (expiry == null) {
            throw new SeedValidationException("The access token carries no expiry.");
        }
        if (expiry.toInstant().plusSeconds(CLOCK_SKEW_SECONDS).isBefore(Instant.now())) {
            throw new SeedValidationException("The access token has expired.");
        }
        return claims;
    }

    /**
     * Loads the token's subject onto the flow, including the email and mobile claims.
     * <p>
     * The subject of an access token is the user id in most configurations and the username in others, so both are
     * tried. The OTP executors refuse to initiate without the claim they send the code to, so the claims are loaded
     * here rather than only the identifier.
     */
    private boolean resolveUser(FlowExecutionContext context, String subject) throws UserStoreException {

        int tenantId = IdentityTenantUtil.getTenantId(context.getTenantDomain());
        AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) StepUpFlowTestingDataHolder.getInstance()
                .getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();

        User user = null;
        try {
            user = userStoreManager.getUserWithID(subject, null, null);
        } catch (UserStoreException e) {
            LOG.debug("The subject is not a user id. Falling back to a username lookup.");
        }
        if (user == null) {
            List<User> users = userStoreManager.getUserListWithID(USERNAME_CLAIM, subject, null);
            if (users == null || users.isEmpty()) {
                return false;
            }
            user = users.get(0);
        }

        context.getFlowUser().setUsername(user.getUsername());
        context.getFlowUser().setUserId(user.getUserID());
        context.getFlowUser().setUserStoreDomain(user.getUserStoreDomain());

        Map<String, String> claims = userStoreManager.getUserClaimValuesWithID(user.getUserID(),
                new String[]{EMAIL_CLAIM, MOBILE_CLAIM}, null);
        if (claims != null) {
            claims.forEach((uri, value) -> {
                if (StringUtils.isNotBlank(value)) {
                    context.getFlowUser().addClaim(uri, value);
                }
            });
        }
        return true;
    }

    private static boolean isEnabled() {

        if (Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY))) {
            return true;
        }
        return Boolean.parseBoolean(IdentityUtil.getProperty(ENABLED_PROPERTY));
    }

    private static String defaulted(String value, String fallback) {

        return StringUtils.isBlank(value) ? fallback : value.trim();
    }

    private static ExecutorResponse error(String message) {

        ExecutorResponse response = new ExecutorResponse();
        response.setResult(STATUS_ERROR);
        response.setErrorCode("SSO-DEV-0002");
        response.setErrorMessage(message);
        response.setErrorDescription(message);
        return response;
    }

    @Override
    public List<String> getInitiationData() {

        return Arrays.asList(INPUT_LAUNCH_TOKEN, INPUT_ACCESS_TOKEN, INPUT_OPERATION_TYPE,
                INPUT_OPERATION_ATTRIBUTES, INPUT_CALLBACK_URL);
    }

    @Override
    public ExecutorResponse rollback(FlowExecutionContext context) {

        return new ExecutorResponse(STATUS_COMPLETE);
    }

    /**
     * Raised when a supplied token cannot be trusted.
     */
    private static class SeedValidationException extends Exception {

        private static final long serialVersionUID = 1L;

        SeedValidationException(String message) {

            super(message);
        }
    }
}
