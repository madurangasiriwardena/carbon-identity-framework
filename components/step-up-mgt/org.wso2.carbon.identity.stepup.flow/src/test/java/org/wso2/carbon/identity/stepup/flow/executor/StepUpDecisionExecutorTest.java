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

package org.wso2.carbon.identity.stepup.flow.executor;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.stepup.flow.internal.component.StepUpFlowComponentServiceHolder;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionServerException;
import org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement;
import org.wso2.carbon.identity.stepup.mgt.api.service.StepUpDecisionService;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.DECISION_EVALUATED;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_ATTRIBUTES;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_TYPE;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OUTSTANDING_FACTORS;

/**
 * Unit tests of {@link StepUpDecisionExecutor}.
 */
public class StepUpDecisionExecutorTest {

    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String EMAIL_OTP = "email-otp-authenticator";
    private static final String SMS_OTP = "sms-otp-authenticator";

    private StepUpDecisionService decisionService;
    private StepUpDecisionExecutor executor;
    private FlowExecutionContext context;

    @BeforeMethod
    public void setUp() {

        decisionService = mock(StepUpDecisionService.class);
        StepUpFlowComponentServiceHolder.getInstance().setStepUpDecisionService(decisionService);
        executor = new StepUpDecisionExecutor();
        context = new FlowExecutionContext();
        context.setTenantDomain("carbon.super");
        context.setProperty(OPERATION_TYPE, "UPDATE_PROFILE");
        context.setProperty(OPERATION_ATTRIBUTES, EMAIL_CLAIM);
    }

    @Test(description = "Unsatisfied factors are recorded for the operation step to check against.")
    public void testUnsatisfiedFactorsAreRecorded() throws Exception {

        when(decisionService.evaluate(any(), any())).thenReturn(Arrays.asList(
                new FactorRequirement(EMAIL_OTP, 0, false),
                new FactorRequirement(SMS_OTP, 300, true)));

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(response.getContextProperties().get(OUTSTANDING_FACTORS), EMAIL_OTP);
        assertEquals(response.getContextProperties().get(DECISION_EVALUATED), Boolean.TRUE);
    }

    @Test(description = "A decision that requires nothing still records that it was evaluated, so that the "
            + "operation step can tell it apart from a decision that never ran.")
    public void testSatisfiedDecisionStillMarksEvaluated() throws Exception {

        when(decisionService.evaluate(any(), any()))
                .thenReturn(Collections.singletonList(new FactorRequirement(EMAIL_OTP, 300, true)));

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(response.getContextProperties().get(OUTSTANDING_FACTORS), "");
        assertEquals(response.getContextProperties().get(DECISION_EVALUATED), Boolean.TRUE);
    }

    @Test(description = "A tenant with no policy leaves nothing outstanding.")
    public void testEmptyDecisionLeavesNothingOutstanding() throws Exception {

        when(decisionService.evaluate(any(), any())).thenReturn(Collections.emptyList());

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(response.getContextProperties().get(OUTSTANDING_FACTORS), "");
    }

    @Test(description = "A flow that was never seeded with an operation has nothing to evaluate.")
    public void testMissingOperationIsRefused() {

        ExecutorResponse response = executor.execute(new FlowExecutionContext());

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(), "SSO-65104");
    }

    @Test(description = "A policy that cannot be read fails the flow rather than silently requiring nothing.")
    public void testDecisionFailureIsRefused() throws Exception {

        when(decisionService.evaluate(any(), any()))
                .thenThrow(new StepUpDecisionServerException("SUP-60001", "boom"));

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(), "SSO-65103");
    }
}
