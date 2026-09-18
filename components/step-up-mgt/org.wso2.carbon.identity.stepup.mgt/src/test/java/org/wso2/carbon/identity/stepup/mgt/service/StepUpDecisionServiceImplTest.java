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

package org.wso2.carbon.identity.stepup.mgt.service;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.stepup.mgt.api.model.AuthenticationContextView;
import org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement;
import org.wso2.carbon.identity.stepup.mgt.api.model.RequiredFactor;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpOperation;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpPolicy;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpRule;
import org.wso2.carbon.identity.stepup.mgt.internal.config.StepUpPolicyProvider;
import org.wso2.carbon.identity.stepup.mgt.internal.service.impl.StepUpDecisionServiceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests of {@link StepUpDecisionServiceImpl}.
 */
public class StepUpDecisionServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-18T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String MOBILE_CLAIM = "http://wso2.org/claims/mobile";
    private static final String USERNAME_CLAIM = "http://wso2.org/claims/username";
    private static final String UPDATE_PROFILE = "UPDATE_PROFILE";

    private static final String EMAIL_OTP = "email-otp-authenticator";
    private static final String SMS_OTP = "sms-otp-authenticator";

    private StepUpPolicyProvider policyProvider;
    private StepUpDecisionServiceImpl decisionService;

    @BeforeMethod
    public void setUp() {

        policyProvider = mock(StepUpPolicyProvider.class);
        decisionService = new StepUpDecisionServiceImpl(policyProvider, CLOCK);
    }

    @Test(description = "A tenant with no step-up policy behaves exactly as it does today.")
    public void testNoConfigurationYieldsEmptyList() throws Exception {

        when(policyProvider.getPolicy()).thenReturn(StepUpPolicy.empty());

        List<FactorRequirement> requirements =
                decisionService.evaluate(operationOn(EMAIL_CLAIM), contextWith(EMAIL_OTP, NOW));

        assertTrue(requirements.isEmpty());
    }

    @Test(description = "A policy whose rules do not match the operation requires nothing.")
    public void testNoMatchingRuleYieldsEmptyList() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(MOBILE_CLAIM), factor(SMS_OTP, 0)));

        List<FactorRequirement> requirements =
                decisionService.evaluate(operationOn(EMAIL_CLAIM), contextWith(EMAIL_OTP, NOW));

        assertTrue(requirements.isEmpty());
    }

    @Test(description = "A factor the context carries no evidence of is unsatisfied.")
    public void testRequiredFactorAbsentFromContextIsUnsatisfied() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                AuthenticationContextView.builder().authTime(NOW).build());

        assertEquals(requirements.size(), 1);
        assertFalse(requirements.get(0).isSatisfied());
    }

    @Test(description = "A factor performed inside its freshness window is satisfied.")
    public void testRequiredFactorWithinMaxAgeIsSatisfied() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                contextWith(EMAIL_OTP, NOW.minusSeconds(100)));

        assertEquals(requirements.size(), 1);
        assertTrue(requirements.get(0).isSatisfied());
        assertEquals(requirements.get(0).getMaxAge(), 300L);
    }

    @Test(description = "A factor performed exactly at the edge of its window is still satisfied.")
    public void testRequiredFactorAtMaxAgeBoundaryIsSatisfied() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                contextWith(EMAIL_OTP, NOW.minusSeconds(300)));

        assertTrue(requirements.get(0).isSatisfied());
    }

    @Test(description = "A factor performed before its freshness window is unsatisfied.")
    public void testRequiredFactorBeyondMaxAgeIsUnsatisfied() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                contextWith(EMAIL_OTP, NOW.minusSeconds(301)));

        assertFalse(requirements.get(0).isSatisfied());
    }

    @Test(description = "A maxAge of zero is never satisfied by prior context, however recent it is.")
    public void testMaxAgeZeroIsNeverSatisfiedByPriorContext() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 0)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                contextWith(EMAIL_OTP, NOW));

        assertEquals(requirements.size(), 1);
        assertEquals(requirements.get(0).getMaxAge(), 0L);
        assertFalse(requirements.get(0).isSatisfied());
    }

    @Test(description = "A context with no authentication time is unsatisfied, and is not an error.")
    public void testNullAuthTimeIsUnsatisfiedNotAnError() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                AuthenticationContextView.builder().addAchievedFactor(EMAIL_OTP).build());

        assertEquals(requirements.size(), 1);
        assertFalse(requirements.get(0).isSatisfied());
    }

    @Test(description = "An ACR the policy does not name is no evidence of the required factor.")
    public void testUnrecognisedAcrIsUnsatisfied() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                AuthenticationContextView.builder()
                        .achievedAcr("urn:acr:unknown")
                        .authTime(NOW.minusSeconds(10))
                        .build());

        assertFalse(requirements.get(0).isSatisfied());
    }

    @Test(description = "A requirement may equally be satisfied by an achieved ACR.")
    public void testAchievedAcrSatisfiesRequirement() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                AuthenticationContextView.builder()
                        .achievedAcr(EMAIL_OTP)
                        .authTime(NOW.minusSeconds(10))
                        .build());

        assertTrue(requirements.get(0).isSatisfied());
    }

    @Test(description = "An authentication time in the future is not treated as fresh.")
    public void testAuthTimeInFutureIsUnsatisfied() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM),
                contextWith(EMAIL_OTP, NOW.plusSeconds(60)));

        assertFalse(requirements.get(0).isSatisfied());
    }

    @Test(description = "Several attributes updated together produce one deduplicated requirement per factor, "
            + "carrying the strictest freshness demanded of it.")
    public void testOverlappingFactorsAreDeduplicatedKeepingStrictestFreshness() throws Exception {

        givenPolicy(
                rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 600)),
                rule(1, Collections.emptySet(), setOf(MOBILE_CLAIM), factor(EMAIL_OTP, 60),
                        factor(SMS_OTP, 900)));

        StepUpOperation operation = StepUpOperation.builder()
                .operationType(UPDATE_PROFILE)
                .addAttribute(EMAIL_CLAIM)
                .addAttribute(MOBILE_CLAIM)
                .build();

        List<FactorRequirement> requirements =
                decisionService.evaluate(operation, contextWith(EMAIL_OTP, NOW.minusSeconds(120)));

        assertEquals(requirements.size(), 2);
        FactorRequirement emailOtp = find(requirements, EMAIL_OTP);
        assertEquals(emailOtp.getMaxAge(), 60L, "The strictest window of the two rules must win.");
        assertFalse(emailOtp.isSatisfied(), "120 seconds ago is outside the 60 second window.");
        assertEquals(find(requirements, SMS_OTP).getMaxAge(), 900L);
    }

    @Test(description = "A rule matching on operation alone applies whatever attributes are touched.")
    public void testRuleMatchingOnOperationAlone() throws Exception {

        givenPolicy(rule(0, setOf(UPDATE_PROFILE), Collections.emptySet(), factor(EMAIL_OTP, 0)));

        StepUpOperation operation = StepUpOperation.builder()
                .operationType(UPDATE_PROFILE)
                .addAttribute(USERNAME_CLAIM)
                .build();

        assertEquals(decisionService.evaluate(operation, contextWith(EMAIL_OTP, NOW)).size(), 1);
    }

    @Test(description = "A rule declaring both an operation and attributes requires both to match.")
    public void testRuleMatchingOnOperationAndAttribute() throws Exception {

        givenPolicy(rule(0, setOf(UPDATE_PROFILE), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 0)));

        StepUpOperation otherOperation = StepUpOperation.builder()
                .operationType("UPDATE_PASSWORD")
                .addAttribute(EMAIL_CLAIM)
                .build();

        assertTrue(decisionService.evaluate(otherOperation, contextWith(EMAIL_OTP, NOW)).isEmpty());
    }

    @Test(description = "A null operation requires nothing rather than failing.")
    public void testNullOperationYieldsEmptyList() throws Exception {

        assertTrue(decisionService.evaluate(null, contextWith(EMAIL_OTP, NOW)).isEmpty());
    }

    @Test(description = "A null context satisfies nothing.")
    public void testNullContextIsUnsatisfied() throws Exception {

        givenPolicy(rule(0, Collections.emptySet(), setOf(EMAIL_CLAIM), factor(EMAIL_OTP, 300)));

        List<FactorRequirement> requirements = decisionService.evaluate(operationOn(EMAIL_CLAIM), null);

        assertEquals(requirements.size(), 1);
        assertFalse(requirements.get(0).isSatisfied());
    }

    private void givenPolicy(StepUpRule... rules) throws Exception {

        when(policyProvider.getPolicy()).thenReturn(new StepUpPolicy(Arrays.asList(rules)));
    }

    private static StepUpRule rule(int index, Set<String> operations, Set<String> attributes,
                                   RequiredFactor... factors) {

        return new StepUpRule(index, operations, attributes, Arrays.asList(factors));
    }

    private static RequiredFactor factor(String factorId, long maxAge) {

        return new RequiredFactor(factorId, maxAge);
    }

    private static StepUpOperation operationOn(String attribute) {

        return StepUpOperation.builder().operationType(UPDATE_PROFILE).addAttribute(attribute).build();
    }

    private static AuthenticationContextView contextWith(String factor, Instant authTime) {

        return AuthenticationContextView.builder().addAchievedFactor(factor).authTime(authTime).build();
    }

    private static Set<String> setOf(String... values) {

        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private static FactorRequirement find(List<FactorRequirement> requirements, String factorId) {

        Optional<FactorRequirement> match = requirements.stream()
                .filter(requirement -> factorId.equals(requirement.getFactorId()))
                .findFirst();
        assertTrue(match.isPresent(), "Expected a requirement for factor: " + factorId);
        return match.get();
    }
}
