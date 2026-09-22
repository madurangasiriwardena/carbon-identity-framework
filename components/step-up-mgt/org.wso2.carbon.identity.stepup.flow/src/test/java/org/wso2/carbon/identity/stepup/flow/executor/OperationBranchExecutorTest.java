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
import org.wso2.carbon.identity.flow.mgt.model.ExecutorDTO;
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;
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
 * action left on the context rather than about the response.
 */
public class OperationBranchExecutorTest {

    private OperationBranchExecutor executor;
    private FlowExecutionContext context;

    @BeforeMethod
    public void setUp() {

        executor = new OperationBranchExecutor();
        context = new FlowExecutionContext();
        context.setContextIdentifier("flow-1");
        Map<String, String> metadata = new HashMap<>();
        metadata.put(StepUpFlowConstants.ExecutorMetadata.BRANCH_ACTION_PREFIX + "mobile", "collect_mobile");
        metadata.put(StepUpFlowConstants.ExecutorMetadata.BRANCH_ACTION_PREFIX + "email", "collect_email");
        context.setCurrentNode(new NodeConfig.Builder()
                .id("collection_router")
                .executorConfig(new ExecutorDTO(StepUpFlowConstants.OPERATION_BRANCH_EXECUTOR, metadata))
                .build());
    }

    @Test(description = "A chosen branch is answered by naming the action that reaches it, which is what lets the "
            + "decision node that follows complete without rendering its page.")
    public void testChosenBranchSetsTheAction() {

        context.setProperty(OPERATION_BRANCH, "email");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(context.getCurrentActionId(), "collect_email");
    }

    @Test(description = "A flow that has committed to no branch is the ordinary way into a choice page, so it "
            + "passes through leaving the choice to the user.")
    public void testUnchosenBranchLeavesTheChoiceAlone() {

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertNull(context.getCurrentActionId());
    }

    @Test(description = "A branch with no action configured is refused. Passing through would render a choice the "
            + "user has already answered, which reads as the answer having been lost.")
    public void testBranchWithNoActionIsRefused() {

        context.setProperty(OPERATION_BRANCH, "password");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(),
                StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_ACTION_FOR_BRANCH.getCode());
        assertNull(context.getCurrentActionId());
    }
}
