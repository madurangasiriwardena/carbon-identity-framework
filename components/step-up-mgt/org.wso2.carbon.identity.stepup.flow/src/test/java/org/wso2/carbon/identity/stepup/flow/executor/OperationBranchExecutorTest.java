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
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;
import org.wso2.carbon.identity.stepup.flow.TestGraphs;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_BRANCH;

/**
 * Unit tests of {@link OperationBranchExecutor}.
 * <p>
 * What this executor does is set the action the engine's decision node reads, so the assertions are about the
 * action left on the context rather than about the response. The flow is built as a graph rather than as
 * configuration, because the graph is now what the executor reads.
 */
public class OperationBranchExecutorTest {

    private static final String MOBILE_CLAIM = "http://wso2.org/claims/mobile";
    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";

    private OperationBranchExecutor executor;
    private FlowExecutionContext context;

    @BeforeMethod
    public void setUp() {

        executor = new OperationBranchExecutor();
        context = new FlowExecutionContext();
        context.setContextIdentifier("flow-1");

        Map<String, String> branches = new HashMap<>();
        branches.put("choose_mobile", "mobile_collection");
        branches.put("choose_email", "email_collection");

        context.setGraphConfig(TestGraphs.builder()
                .node("operation_router", "operation_choice")
                .decision("operation_choice", branches)
                .page("mobile_collection", "operation_completion", MOBILE_CLAIM)
                .page("email_collection", "operation_completion", EMAIL_CLAIM)
                .node("operation_completion", "END")
                .build());

        NodeConfig router = new NodeConfig.Builder().id("operation_router").build();
        router.setNextNodeId("operation_choice");
        context.setCurrentNode(router);
    }

    @Test(description = "The attribute the caller asked for is answered by naming the action whose branch collects "
            + "it, which is what lets the decision node that follows complete without rendering its page.")
    public void testRequestedAttributeSetsTheAction() {

        context.setProperty(OPERATION_BRANCH, EMAIL_CLAIM);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(context.getCurrentActionId(), "choose_email");
    }

    @Test(description = "A branch is found through an authentication step placed ahead of the page that collects, "
            + "so a factor specific to one attribute does not hide the branch it belongs to.")
    public void testBranchFoundBehindAnEarlierStep() {

        Map<String, String> branches = new HashMap<>();
        branches.put("choose_mobile", "mobile_otp");

        context.setGraphConfig(TestGraphs.builder()
                .node("operation_router", "operation_choice")
                .decision("operation_choice", branches)
                .node("mobile_otp", "mobile_collection")
                .page("mobile_collection", "operation_completion", MOBILE_CLAIM)
                .node("operation_completion", "END")
                .build());
        context.setProperty(OPERATION_BRANCH, MOBILE_CLAIM);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(context.getCurrentActionId(), "choose_mobile");
    }

    @Test(description = "An attribute no branch collects is refused, rather than passing through to a choice page "
            + "that could not satisfy the request either.")
    public void testAttributeNoBranchCollectsIsRefused() {

        context.setProperty(OPERATION_BRANCH, "http://wso2.org/claims/nickname");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(),
                StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_BRANCH_FOR_ATTRIBUTE.getCode());
        assertNull(context.getCurrentActionId());
    }

    @Test(description = "A flow where more than one branch collects the attribute is refused rather than resolved "
            + "arbitrarily: which branch the caller meant is genuinely unknown.")
    public void testAmbiguousBranchIsRefused() {

        Map<String, String> branches = new HashMap<>();
        branches.put("choose_one", "first_collection");
        branches.put("choose_two", "second_collection");

        context.setGraphConfig(TestGraphs.builder()
                .node("operation_router", "operation_choice")
                .decision("operation_choice", branches)
                .page("first_collection", "operation_completion", MOBILE_CLAIM)
                .page("second_collection", "operation_completion", MOBILE_CLAIM)
                .node("operation_completion", "END")
                .build());
        context.setProperty(OPERATION_BRANCH, MOBILE_CLAIM);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(),
                StepUpFlowConstants.ErrorMessages.ERROR_CODE_AMBIGUOUS_BRANCH.getCode());
    }

    @Test(description = "A flow that has committed to no branch is the ordinary way into a choice page, so it "
            + "passes through leaving the choice to the user.")
    public void testUnchosenBranchLeavesTheChoiceAlone() {

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertNull(context.getCurrentActionId());
    }

}
