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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.metadata.FlowExecutorMetadata;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.Constants.FlowTypes;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ErrorMessages;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ExecutorMetadata;
import org.wso2.carbon.identity.stepup.flow.util.StepUpFlowContextUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;

/**
 * Answers a branch of the flow from what has already been decided, instead of asking the user.
 * <p>
 * The engine's decision node completes silently when an action has already been chosen and prompts otherwise, so
 * this node placed before one turns that page into a fallback: it renders only for a flow that has not committed to
 * a branch yet.
 * <p>
 * Two of these are expected in a flow that steps up before collecting. The first stands before the operation choice
 * and skips it when the launching request already named the branch. The second stands after the step-up, before the
 * branch is picked up again for collection, where a branch is always known — the flow cannot have reached the
 * step-up without one.
 * <p>
 * A flow with no branch chosen is therefore not an error here: it is the ordinary way into the choice page. A
 * branch that is chosen but maps to no action is, because a flow that asks the same question twice looks to the
 * user like the first answer was lost.
 * <p>
 * The branch to action mapping is read from this node's own configuration, under keys prefixed {@code branch.}.
 * Values are identifiers of the actions in the step that follows, which for a button is the identifier of the
 * button component.
 */
public class OperationBranchExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(OperationBranchExecutor.class);

    @Override
    public String getName() {

        return StepUpFlowConstants.OPERATION_BRANCH_EXECUTOR;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        String branch = StepUpFlowContextUtils.getOperationBranch(context);
        if (StringUtils.isBlank(branch)) {
            // Nothing has committed to a branch, so there is nothing to answer and the choice that follows stands.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Flow " + context.getContextIdentifier() + " has chosen no branch. Leaving the choice that "
                        + "follows to be answered by the user.");
            }
            return new ExecutorResponse(STATUS_COMPLETE);
        }

        String actionId = StepUpFlowContextUtils.getExecutorMetadata(context,
                ExecutorMetadata.BRANCH_ACTION_PREFIX + branch);
        if (StringUtils.isBlank(actionId)) {
            LOG.warn("No action configured for branch " + branch + " of flow "
                    + context.getContextIdentifier() + ".");
            return errorResponse(ErrorMessages.ERROR_CODE_NO_ACTION_FOR_BRANCH,
                    String.format(ErrorMessages.ERROR_CODE_NO_ACTION_FOR_BRANCH.getDescription(), branch));
        }

        /*
         Setting the action is what lets the decision node that follows resolve without prompting. It survives to
         that node because only the decision and page prompt nodes clear it, and neither runs in between. It does
         not survive a page, which is why this node stands immediately before the fan-out rather than earlier.
         */
        context.setCurrentActionId(actionId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Routing flow " + context.getContextIdentifier() + " to branch " + branch + " through action "
                    + actionId + ".");
        }
        return new ExecutorResponse(STATUS_COMPLETE);
    }

    /**
     * Describes this executor to the flow composer.
     * <p>
     * Without it the composer has no entry for the executor and falls back to labelling the step
     * generically, so every execution step in a flow reads the same.
     *
     * @return Executor metadata.
     */
    @Override
    public FlowExecutorMetadata getExecutorMetadata() {

        return FlowExecutorMetadata.builder()
                .displayName("Continue to the chosen operation")
                .description("Answers the choice that follows from the operation the launching request asked for, so "
                        + "the user is not asked again.")
                .build();
    }

    @Override
    public List<String> getInitiationData() {

        return Collections.emptyList();
    }

    @Override
    public ExecutorResponse rollback(FlowExecutionContext context) {

        return new ExecutorResponse(STATUS_COMPLETE);
    }

    @Override
    public Set<FlowTypes> getSupportedFlowTypes() {

        return EnumSet.of(FlowTypes.SECURED_SELF_SERVICE_OPERATIONS);
    }

    private ExecutorResponse errorResponse(ErrorMessages error, String description) {

        ExecutorResponse response = new ExecutorResponse();
        response.setResult(STATUS_ERROR);
        response.setErrorCode(error.getCode());
        response.setErrorMessage(error.getMessage());
        response.setErrorDescription(description);
        return response;
    }
}
