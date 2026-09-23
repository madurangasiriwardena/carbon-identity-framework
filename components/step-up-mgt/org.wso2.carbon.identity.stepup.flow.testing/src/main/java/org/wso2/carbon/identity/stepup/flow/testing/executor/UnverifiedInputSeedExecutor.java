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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.Constants.FlowTypes;
import org.wso2.carbon.identity.stepup.flow.testing.internal.StepUpFlowTestingDataHolder;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_USER_INPUT_REQUIRED;

/**
 * Seeds a secured self-service operations flow from request input, taking every value on trust.
 * <p>
 * This is a stand-in for the real seeding step, which restores prior authentication evidence from a launch token it
 * validates. It exists so the rest of the flow can be exercised before that token has been built, and it is deleted
 * once it has been.
 * <p>
 * It performs no verification whatsoever, so it must never reach a deployment that matters: the caller chooses the
 * subject, the prior authentication evidence and the callback. It is kept out of the shipped feature for that
 * reason, and refuses to run unless {@code StepUp.AllowUnverifiedSeed} has been explicitly turned on.
 */
public class UnverifiedInputSeedExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(UnverifiedInputSeedExecutor.class);

    private static final String EXECUTOR_NAME = "UnverifiedInputSeedExecutor";
    private static final String ENABLED_PROPERTY = "StepUp.AllowUnverifiedSeed";

    private static final String INPUT_USERNAME = "username";
    private static final String INPUT_OPERATION_TYPE = "operationType";
    private static final String INPUT_OPERATION_ATTRIBUTES = "operationAttributes";
    private static final String INPUT_INHERITED_ACR = "inheritedAcr";
    private static final String INPUT_INHERITED_FACTORS = "inheritedFactors";
    private static final String INPUT_INHERITED_AUTH_TIME = "inheritedAuthTime";

    private static final String USERNAME_CLAIM = "http://wso2.org/claims/username";
    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String MOBILE_CLAIM = "http://wso2.org/claims/mobile";

    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        if (!isEnabled()) {
            LOG.error("Refusing to seed a flow from unverified input. Start the server with -D" + ENABLED_PROPERTY
                    + "=true to allow this on a development server.");
            return error("Unverified seeding is not enabled on this server.");
        }

        Map<String, String> inputs = context.getUserInputData();
        String username = inputs == null ? null : inputs.get(INPUT_USERNAME);
        if (StringUtils.isBlank(username)) {
            ExecutorResponse response = new ExecutorResponse(STATUS_USER_INPUT_REQUIRED);
            response.setRequiredData(Collections.singletonList(INPUT_USERNAME));
            return response;
        }

        LOG.warn("Seeding flow " + context.getContextIdentifier() + " from unverified input.");

        try {
            if (!resolveUser(context, username)) {
                return error("No user found for the seeded username.");
            }
        } catch (UserStoreException e) {
            LOG.error("Error while resolving the seeded user.", e);
            return error("Error while resolving the seeded user.");
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("stepUp.operationType", defaulted(inputs.get(INPUT_OPERATION_TYPE), "UPDATE_PROFILE"));
        properties.put("stepUp.operationAttributes", defaulted(inputs.get(INPUT_OPERATION_ATTRIBUTES), EMAIL_CLAIM));
        putIfPresent(properties, "stepUp.inheritedAcr", inputs.get(INPUT_INHERITED_ACR));
        putIfPresent(properties, "stepUp.inheritedFactors", inputs.get(INPUT_INHERITED_FACTORS));
        putIfPresent(properties, "stepUp.inheritedAuthTime", inputs.get(INPUT_INHERITED_AUTH_TIME));

        ExecutorResponse response = new ExecutorResponse(STATUS_COMPLETE);
        response.setContextProperty(properties);
        return response;
    }

    /**
     * Loads the seeded user onto the flow, including the email and mobile claims.
     * <p>
     * The OTP executors refuse to initiate unless the flow user already carries the claim they send the code to, so
     * establishing the subject alone is not enough here.
     */
    private boolean resolveUser(FlowExecutionContext context, String username) throws UserStoreException {

        int tenantId = IdentityTenantUtil.getTenantId(context.getTenantDomain());
        AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) StepUpFlowTestingDataHolder.getInstance()
                .getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();

        List<User> users = userStoreManager.getUserListWithID(USERNAME_CLAIM, username, null);
        if (users == null || users.isEmpty()) {
            return false;
        }
        User user = users.get(0);
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

    /**
     * Whether this server permits seeding a flow from unverified input.
     * <p>
     * A system property is read first because it needs no configuration template: start the server with
     * {@code -DStepUp.AllowUnverifiedSeed=true}. The identity configuration is also honoured for a server that has
     * been set up to carry the element.
     */
    private static boolean isEnabled() {

        if (Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY))) {
            return true;
        }
        return Boolean.parseBoolean(IdentityUtil.getProperty(ENABLED_PROPERTY));
    }

    private static String defaulted(String value, String fallback) {

        return StringUtils.isBlank(value) ? fallback : value.trim();
    }

    private static void putIfPresent(Map<String, Object> properties, String key, String value) {

        if (StringUtils.isNotBlank(value)) {
            properties.put(key, value.trim());
        }
    }

    private static ExecutorResponse error(String message) {

        ExecutorResponse response = new ExecutorResponse();
        response.setResult(STATUS_ERROR);
        response.setErrorCode("SSO-DEV-0001");
        response.setErrorMessage(message);
        response.setErrorDescription(message);
        return response;
    }

    @Override
    public List<String> getInitiationData() {

        return Arrays.asList(INPUT_USERNAME, INPUT_OPERATION_TYPE, INPUT_OPERATION_ATTRIBUTES);
    }

    @Override
    public ExecutorResponse rollback(FlowExecutionContext context) {

        return new ExecutorResponse(STATUS_COMPLETE);
    }

    @Override
    public Set<FlowTypes> getSupportedFlowTypes() {

        return EnumSet.of(FlowTypes.SECURED_SELF_SERVICE_OPERATIONS);
    }
}
