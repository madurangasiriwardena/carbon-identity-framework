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
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.Constants.FlowTypes;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ErrorMessages;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ExecutorMetadata;
import org.wso2.carbon.identity.stepup.flow.util.StepUpFlowContextUtils;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpOperation;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;

/**
 * Fixes which operation the flow performs, on the branch the flow took.
 * <p>
 * One of these sits at the head of each branch of the operation choice. The operation it declares is written to the
 * context once, and everything downstream — the step-up decision and the update itself — reads it from there rather
 * than from the request, so that what was evaluated and what is applied cannot come apart.
 * <p>
 * Two ways of arriving here are expected, and they must behave differently:
 * <ul>
 *     <li><b>Nothing seeded.</b> The launching request named no operation, the choice page was shown, and the user
 *     picked. Whatever this branch declares becomes the operation.</li>
 *     <li><b>Already seeded.</b> The launching request named the operation, the seed recorded it, and the choice
 *     page was skipped by preselecting this branch. This node then merely confirms what is already recorded.</li>
 * </ul>
 * A third way of arriving here is not expected and is refused: the flow was seeded for one operation and the branch
 * reached declares another. The action identifier that chooses a branch travels in the request, so reaching the
 * wrong branch is something a caller can attempt; without this check a user could satisfy the weakest factor a
 * policy requires and then steer into the branch of an attribute that requires a stronger one.
 */
public class OperationSelectionExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(OperationSelectionExecutor.class);

    @Override
    public String getName() {

        return StepUpFlowConstants.OPERATION_SELECTION_EXECUTOR;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        Map<String, String> metadata = StepUpFlowContextUtils.getExecutorMetadata(context);
        String operationType = StringUtils.trimToNull(metadata.get(ExecutorMetadata.OPERATION_TYPE));
        List<String> attributes =
                StepUpFlowContextUtils.splitValues(metadata.get(ExecutorMetadata.OPERATION_ATTRIBUTES));
        String branch = StringUtils.trimToNull(metadata.get(ExecutorMetadata.BRANCH));

        if (StringUtils.isBlank(operationType) && attributes.isEmpty()) {
            // A branch that declares no operation cannot fix one, and letting the flow continue would leave the
            // update step to decide for itself what it is allowed to write.
            return errorResponse(ErrorMessages.ERROR_CODE_NO_BRANCH_CONFIGURED,
                    ErrorMessages.ERROR_CODE_NO_BRANCH_CONFIGURED.getDescription());
        }

        StepUpOperation seeded = StepUpFlowContextUtils.getSeededOperation(context);
        if (seeded != null && !matches(seeded, operationType, attributes)) {
            LOG.warn("Refusing branch " + branch + " of flow " + context.getContextIdentifier()
                    + ": the flow was seeded for a different operation.");
            return errorResponse(ErrorMessages.ERROR_CODE_BRANCH_CONFLICTS_WITH_SEED,
                    String.format(ErrorMessages.ERROR_CODE_BRANCH_CONFLICTS_WITH_SEED.getDescription(),
                            describe(operationType, attributes),
                            describe(seeded.getOperationType(), seeded.getAttributes())));
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextProperties.OPERATION_TYPE, operationType);
        properties.put(ContextProperties.OPERATION_ATTRIBUTES, StepUpFlowContextUtils.joinValues(attributes));
        if (StringUtils.isNotBlank(branch)) {
            properties.put(ContextProperties.OPERATION_BRANCH, branch);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Flow " + context.getContextIdentifier() + " will perform "
                    + describe(operationType, attributes) + (seeded == null ? " as chosen by the user."
                    : " as it was seeded to."));
        }

        ExecutorResponse response = new ExecutorResponse(STATUS_COMPLETE);
        response.setContextProperty(properties);
        return response;
    }

    /**
     * Whether an operation already recorded on the context is the one this branch performs.
     * <p>
     * The attributes decide it. The operation type is compared too, but a flow may leave it unset on either side and
     * an absent type is not treated as a disagreement.
     */
    private boolean matches(StepUpOperation seeded, String operationType, List<String> attributes) {

        Set<String> seededAttributes = seeded.getAttributes() == null
                ? Collections.emptySet() : seeded.getAttributes();
        if (!seededAttributes.equals(new LinkedHashSet<>(attributes))) {
            return false;
        }
        return StringUtils.isBlank(operationType) || StringUtils.isBlank(seeded.getOperationType())
                || operationType.equals(seeded.getOperationType());
    }

    private String describe(String operationType, Collection<String> attributes) {

        String joined = StepUpFlowContextUtils.joinValues(attributes);
        if (StringUtils.isBlank(operationType)) {
            return joined;
        }
        return StringUtils.isBlank(joined) ? operationType : operationType + " on " + joined;
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
