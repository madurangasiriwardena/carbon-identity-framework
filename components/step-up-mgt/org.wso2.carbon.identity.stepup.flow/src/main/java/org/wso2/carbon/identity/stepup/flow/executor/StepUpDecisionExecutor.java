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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.metadata.FlowExecutorMetadata;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.Constants.FlowTypes;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants;
import org.wso2.carbon.identity.stepup.flow.internal.component.StepUpFlowComponentServiceHolder;
import org.wso2.carbon.identity.stepup.flow.util.StepUpFlowContextUtils;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionException;
import org.wso2.carbon.identity.stepup.mgt.api.model.AuthenticationContextView;
import org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpOperation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.DECISION_EVALUATED;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OUTSTANDING_FACTORS;

/**
 * Asks the step-up decision component what this flow's operation requires, and records what is still outstanding.
 * <p>
 * The decision is taken here, against the evidence the flow was seeded with, and never again. Evaluating later would
 * be evaluating against a context that has since been advanced by the step-up itself, at which point the original
 * factor's age is no longer recoverable.
 */
public class StepUpDecisionExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(StepUpDecisionExecutor.class);

    @Override
    public String getName() {

        return StepUpFlowConstants.STEP_UP_DECISION_EXECUTOR;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        StepUpOperation operation = StepUpFlowContextUtils.getSeededOperation(context);
        if (operation == null) {
            return errorResponse(StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_OPERATION_SEEDED,
                    StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_OPERATION_SEEDED.getDescription());
        }

        AuthenticationContextView inheritedEvidence = StepUpFlowContextUtils.getInheritedEvidence(context);
        List<FactorRequirement> requirements;
        try {
            requirements = StepUpFlowComponentServiceHolder.getInstance().getStepUpDecisionService()
                    .evaluate(operation, inheritedEvidence);
        } catch (StepUpDecisionException e) {
            LOG.error("Error while evaluating the step-up decision for flow: " + context.getContextIdentifier(), e);
            return errorResponse(StepUpFlowConstants.ErrorMessages.ERROR_CODE_EVALUATING_DECISION,
                    String.format(StepUpFlowConstants.ErrorMessages.ERROR_CODE_EVALUATING_DECISION.getDescription(),
                            context.getTenantDomain()));
        }

        List<String> outstanding = requirements.stream()
                .filter(requirement -> !requirement.isSatisfied())
                .map(FactorRequirement::getFactorId)
                .distinct()
                .collect(Collectors.toList());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Step-up decision for flow " + context.getContextIdentifier() + " left " + outstanding.size()
                    + " factor(s) outstanding.");
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(OUTSTANDING_FACTORS, StepUpFlowContextUtils.joinValues(outstanding));
        properties.put(DECISION_EVALUATED, Boolean.TRUE);

        ExecutorResponse response = new ExecutorResponse(STATUS_COMPLETE);
        response.setContextProperty(properties);
        return response;
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
                .displayName("Evaluate the step-up policy")
                .description("Records which factors the tenant policy still requires, given the evidence the flow was "
                        + "seeded with.")
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

    private ExecutorResponse errorResponse(StepUpFlowConstants.ErrorMessages error, String description) {

        ExecutorResponse response = new ExecutorResponse();
        response.setResult(STATUS_ERROR);
        response.setErrorCode(error.getCode());
        response.setErrorMessage(error.getMessage());
        response.setErrorDescription(description);
        return response;
    }
}
