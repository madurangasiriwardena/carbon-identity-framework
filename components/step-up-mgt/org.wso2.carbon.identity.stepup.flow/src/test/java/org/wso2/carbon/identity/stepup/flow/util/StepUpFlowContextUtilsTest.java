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

package org.wso2.carbon.identity.stepup.flow.util;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.stepup.flow.TestExecutors;
import org.wso2.carbon.identity.stepup.flow.internal.component.StepUpFlowComponentServiceHolder;
import org.wso2.carbon.identity.stepup.mgt.api.model.AuthenticationContextView;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpOperation;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.DECISION_EVALUATED;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.INHERITED_ACR;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.INHERITED_AUTH_TIME;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.INHERITED_FACTORS;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_ATTRIBUTES;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_TYPE;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OUTSTANDING_FACTORS;

/**
 * Unit tests of {@link StepUpFlowContextUtils}.
 */
public class StepUpFlowContextUtilsTest {

    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String EMAIL_OTP = "email-otp-authenticator";

    private FlowExecutionContext context;

    @BeforeMethod
    public void setUp() {

        context = new FlowExecutionContext();
    }

    @Test(description = "Inherited evidence is read back from the context properties.")
    public void testInheritedEvidenceIsRead() {

        context.setProperty(INHERITED_ACR, "urn:acr:silver");
        context.setProperty(INHERITED_FACTORS, "BasicAuthenticator,totp-authenticator");
        context.setProperty(INHERITED_AUTH_TIME, "2026-09-18T09:00:00Z");

        AuthenticationContextView view = StepUpFlowContextUtils.getInheritedEvidence(context);

        assertEquals(view.getAchievedAcr().orElse(null), "urn:acr:silver");
        assertEquals(view.getAchievedFactors().size(), 2);
        assertTrue(view.hasFactor("totp-authenticator"));
        assertEquals(view.getAuthTime().orElse(null), Instant.parse("2026-09-18T09:00:00Z"));
    }

    @Test(description = "A multi valued property that survived the JSON context store as a list reads the same as "
            + "one that survived as a separated string.")
    public void testMultiValuedPropertyReadsFromListForm() {

        context.setProperty(INHERITED_FACTORS, Arrays.asList("BasicAuthenticator", "totp-authenticator"));

        AuthenticationContextView view = StepUpFlowContextUtils.getInheritedEvidence(context);

        assertEquals(view.getAchievedFactors().size(), 2);
        assertTrue(view.hasFactor("BasicAuthenticator"));
    }

    @Test(description = "An unreadable authentication time is no known authentication event, which fails every "
            + "freshness check rather than failing the flow.")
    public void testUnparseableAuthTimeIsTreatedAsAbsent() {

        context.setProperty(INHERITED_AUTH_TIME, "whenever");

        assertFalse(StepUpFlowContextUtils.getInheritedEvidence(context).getAuthTime().isPresent());
    }

    @Test(description = "A context with no seeded evidence yields an empty view rather than failing.")
    public void testEmptyContextYieldsEmptyEvidence() {

        AuthenticationContextView view = StepUpFlowContextUtils.getInheritedEvidence(context);

        assertFalse(view.getAchievedAcr().isPresent());
        assertFalse(view.getAuthTime().isPresent());
        assertTrue(view.getAchievedFactors().isEmpty());
    }

    @Test(description = "The seeded operation is read back from the context properties.")
    public void testSeededOperationIsRead() {

        context.setProperty(OPERATION_TYPE, "UPDATE_PROFILE");
        context.setProperty(OPERATION_ATTRIBUTES, EMAIL_CLAIM);

        StepUpOperation operation = StepUpFlowContextUtils.getSeededOperation(context);

        assertEquals(operation.getOperationType(), "UPDATE_PROFILE");
        assertEquals(operation.getAttributes(), Collections.singleton(EMAIL_CLAIM));
    }

    @Test(description = "A flow with no seeded operation carries none.")
    public void testAbsentOperationIsNull() {

        assertNull(StepUpFlowContextUtils.getSeededOperation(context));
    }

    @Test(description = "A decision that never ran is not reported as evaluated.")
    public void testDecisionEvaluatedDefaultsToFalse() {

        assertFalse(StepUpFlowContextUtils.isDecisionEvaluated(context));
    }

    @Test(description = "The evaluated marker is read whether it survived as a boolean or as its string form.")
    public void testDecisionEvaluatedReadsBothForms() {

        context.setProperty(DECISION_EVALUATED, Boolean.TRUE);
        assertTrue(StepUpFlowContextUtils.isDecisionEvaluated(context));

        context.setProperty(DECISION_EVALUATED, "true");
        assertTrue(StepUpFlowContextUtils.isDecisionEvaluated(context));
    }

    @Test(description = "Outstanding factors are read back from the context properties.")
    public void testOutstandingFactorsAreRead() {

        context.setProperty(OUTSTANDING_FACTORS, EMAIL_OTP);

        assertEquals(StepUpFlowContextUtils.getOutstandingFactors(context), Collections.singletonList(EMAIL_OTP));
    }

    @Test(description = "Only an authentication executor that completed contributes a performed factor.")
    public void testOnlyAuthenticationExecutorsContributeFactors() {

        StepUpFlowComponentServiceHolder holder = StepUpFlowComponentServiceHolder.getInstance();
        TestExecutors.TestAuthenticationExecutor otpExecutor =
                new TestExecutors.TestAuthenticationExecutor("TestOTPExecutor", EMAIL_OTP);
        TestExecutors.TestPlainExecutor plainExecutor = new TestExecutors.TestPlainExecutor("TestPlainExecutor");
        holder.addExecutor(otpExecutor);
        holder.addExecutor(plainExecutor);
        try {
            context.setCompletedNodes(Arrays.asList(
                    TestExecutors.nodeFor("TestOTPExecutor"), TestExecutors.nodeFor("TestPlainExecutor")));

            Set<String> performed = StepUpFlowContextUtils.getFactorsCompletedInFlow(context);

            assertEquals(performed, Collections.singleton(EMAIL_OTP));
        } finally {
            holder.removeExecutor(otpExecutor);
            holder.removeExecutor(plainExecutor);
        }
    }

    @Test(description = "A flow that completed no nodes performed no factors.")
    public void testNoCompletedNodesMeansNoFactors() {

        assertTrue(StepUpFlowContextUtils.getFactorsCompletedInFlow(context).isEmpty());
    }

}
