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
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_ATTRIBUTES;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_BRANCH;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_TYPE;

/**
 * Unit tests of {@link OperationSelectionExecutor}.
 * <p>
 * The case worth the most attention is the conflict: a flow seeded for one attribute that arrives on the branch of
 * another. The action that picks a branch travels in the request, so this is reachable by a caller, and allowing it
 * would let the factor required for a weakly protected attribute stand in for the factor required by a strongly
 * protected one.
 */
public class OperationSelectionExecutorTest {

    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String MOBILE_CLAIM = "http://wso2.org/claims/mobile";
    private static final String UPDATE_PROFILE = "UPDATE_PROFILE";

    private OperationSelectionExecutor executor;
    private FlowExecutionContext context;

    @BeforeMethod
    public void setUp() {

        executor = new OperationSelectionExecutor();
        context = new FlowExecutionContext();
        context.setContextIdentifier("flow-1");
    }

    @Test(description = "A flow with nothing seeded takes the operation from the branch the user chose.")
    public void testUnseededFlowTakesTheBranchOperation() {

        onBranch("mobile", UPDATE_PROFILE, MOBILE_CLAIM);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(response.getContextProperties().get(OPERATION_TYPE), UPDATE_PROFILE);
        assertEquals(response.getContextProperties().get(OPERATION_ATTRIBUTES), MOBILE_CLAIM);
        assertEquals(response.getContextProperties().get(OPERATION_BRANCH), "mobile");
    }

    @Test(description = "A flow seeded for the operation this branch performs is confirmed, not refused. This is "
            + "the ordinary path for a launch that named its branch and so skipped the choice page.")
    public void testSeededFlowOnItsOwnBranchIsConfirmed() {

        context.setProperty(OPERATION_TYPE, UPDATE_PROFILE);
        context.setProperty(OPERATION_ATTRIBUTES, MOBILE_CLAIM);
        onBranch("mobile", UPDATE_PROFILE, MOBILE_CLAIM);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        assertEquals(response.getContextProperties().get(OPERATION_ATTRIBUTES), MOBILE_CLAIM);
    }

    @Test(description = "A flow seeded for one attribute that arrives on the branch of another is refused, rather "
            + "than having the seeded operation quietly overwritten by the branch.")
    public void testSeededFlowOnAnotherBranchIsRefused() {

        context.setProperty(OPERATION_TYPE, UPDATE_PROFILE);
        context.setProperty(OPERATION_ATTRIBUTES, MOBILE_CLAIM);
        onBranch("email", UPDATE_PROFILE, EMAIL_CLAIM);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(),
                StepUpFlowConstants.ErrorMessages.ERROR_CODE_BRANCH_CONFLICTS_WITH_SEED.getCode());
        assertNull(response.getContextProperties());
    }

    @Test(description = "A branch that declares no operation cannot fix one, so it fails rather than leaving the "
            + "update step to decide for itself what it may write.")
    public void testBranchWithNoOperationIsRefused() {

        onBranch("mobile", null, null);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(),
                StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_BRANCH_CONFIGURED.getCode());
    }

    @Test(description = "A flow definition that leaves the operation type off both sides is not a disagreement; "
            + "the attributes are what identify the operation.")
    public void testAbsentOperationTypeIsNotAConflict() {

        context.setProperty(OPERATION_ATTRIBUTES, MOBILE_CLAIM);
        onBranch("mobile", null, MOBILE_CLAIM);

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
    }

    private void onBranch(String branch, String operationType, String attributes) {

        Map<String, String> metadata = new HashMap<>();
        if (branch != null) {
            metadata.put(StepUpFlowConstants.ExecutorMetadata.BRANCH, branch);
        }
        if (operationType != null) {
            metadata.put(StepUpFlowConstants.ExecutorMetadata.OPERATION_TYPE, operationType);
        }
        if (attributes != null) {
            metadata.put(StepUpFlowConstants.ExecutorMetadata.OPERATION_ATTRIBUTES, attributes);
        }
        NodeConfig node = new NodeConfig.Builder()
                .id("select_" + branch)
                .executorConfig(new ExecutorDTO(StepUpFlowConstants.OPERATION_SELECTION_EXECUTOR, metadata))
                .build();
        context.setCurrentNode(node);
    }
}
