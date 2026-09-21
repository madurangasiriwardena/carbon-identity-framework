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
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.Constants.FlowTypes;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants;
import org.wso2.carbon.identity.stepup.flow.internal.component.StepUpFlowComponentServiceHolder;
import org.wso2.carbon.identity.stepup.flow.util.StepUpFlowContextUtils;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_USER_INPUT_REQUIRED;

/**
 * Applies the protected update this flow was seeded for.
 * <p>
 * Before applying anything it re-checks the seed-time decision against the factors actually performed in this flow,
 * and refuses to run if any required factor is still outstanding. The check is deliberately made here rather than
 * relying on the flow definition placing the step-up ahead of this step: a flow authored with the steps in the wrong
 * order must fail closed, not apply the update.
 * <p>
 * Only the attributes the flow was seeded for are applied. A value supplied for anything else is ignored rather than
 * written, so a caller cannot widen the operation beyond what was evaluated.
 */
public class SecuredOperationExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(SecuredOperationExecutor.class);

    @Override
    public String getName() {

        return StepUpFlowConstants.SECURED_OPERATION_EXECUTOR;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        if (!StepUpFlowContextUtils.isDecisionEvaluated(context)) {
            // Never evaluated is not the same as evaluated and found to require nothing.
            return errorResponse(STATUS_ERROR, StepUpFlowConstants.ErrorMessages.ERROR_CODE_DECISION_NOT_EVALUATED,
                    StepUpFlowConstants.ErrorMessages.ERROR_CODE_DECISION_NOT_EVALUATED.getDescription());
        }

        List<String> outstanding = new ArrayList<>(StepUpFlowContextUtils.getOutstandingFactors(context));
        outstanding.removeAll(StepUpFlowContextUtils.getFactorsCompletedInFlow(context));
        if (!outstanding.isEmpty()) {
            LOG.warn("Refusing the secured operation for flow " + context.getContextIdentifier()
                    + ": required factors were not performed.");
            return errorResponse(STATUS_ERROR, StepUpFlowConstants.ErrorMessages.ERROR_CODE_STEP_UP_NOT_SATISFIED,
                    String.format(StepUpFlowConstants.ErrorMessages.ERROR_CODE_STEP_UP_NOT_SATISFIED.getDescription(),
                            StepUpFlowContextUtils.joinValues(outstanding)));
        }

        if (context.getFlowUser() == null || StringUtils.isBlank(context.getFlowUser().getUserId())) {
            return errorResponse(STATUS_ERROR, StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_SUBJECT,
                    StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_SUBJECT.getDescription());
        }

        Map<String, String> updates = collectSeededAttributeValues(context);
        if (updates.isEmpty()) {
            // The user has reached the operation step without supplying a value. Ask rather than fail.
            // This is also what a flow whose form collects a different attribute from the one it was seeded for
            // looks like, so both are named here rather than leaving the step to repeat silently.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Nothing to apply for flow " + context.getContextIdentifier() + ". Seeded attributes: "
                        + StepUpFlowContextUtils.joinValues(seededAttributes(context)) + ". Supplied inputs: "
                        + (context.getUserInputData() == null ? ""
                        : StepUpFlowContextUtils.joinValues(context.getUserInputData().keySet())));
            }
            return userInputRequiredResponse(context);
        }

        try {
            applyUpdate(context, updates);
        } catch (UserStoreException e) {
            LOG.error("Error while applying the secured operation for flow: " + context.getContextIdentifier(), e);
            return errorResponse(STATUS_ERROR, StepUpFlowConstants.ErrorMessages.ERROR_CODE_APPLYING_OPERATION,
                    String.format(StepUpFlowConstants.ErrorMessages.ERROR_CODE_APPLYING_OPERATION.getDescription(),
                            context.getTenantDomain()));
        }

        ExecutorResponse response = new ExecutorResponse(STATUS_COMPLETE);
        response.setUpdatedUserClaims(new HashMap<>(updates));
        return response;
    }

    /**
     * Applies the update through the ordinary user store path, so that everything that normally observes a profile
     * update still observes this one. Notably, the verification handlers that hold a new email address or mobile
     * number as a pending value until the user proves control of it are triggered from here, which is why this flow
     * does not carry a proof-of-control ceremony of its own.
     */
    private void applyUpdate(FlowExecutionContext context, Map<String, String> updates) throws UserStoreException {

        int tenantId = IdentityTenantUtil.getTenantId(context.getTenantDomain());
        UserStoreManager userStoreManager = (UserStoreManager) StepUpFlowComponentServiceHolder.getInstance()
                .getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
        ((AbstractUserStoreManager) userStoreManager)
                .setUserClaimValuesWithID(context.getFlowUser().getUserId(), new HashMap<>(updates), null);
    }

    /**
     * Picks out the values supplied for the attributes this flow was seeded for, and nothing else.
     */
    private Map<String, String> collectSeededAttributeValues(FlowExecutionContext context) {

        Map<String, String> updates = new LinkedHashMap<>();
        Set<String> seededAttributes = seededAttributes(context);
        if (seededAttributes.isEmpty() || context.getUserInputData() == null) {
            return updates;
        }
        for (String attribute : seededAttributes) {
            String value = context.getUserInputData().get(attribute);
            if (StringUtils.isNotBlank(value)) {
                updates.put(attribute, value);
            }
        }
        return updates;
    }

    private Set<String> seededAttributes(FlowExecutionContext context) {

        return StepUpFlowContextUtils.getSeededOperation(context) == null
                ? Collections.emptySet()
                : StepUpFlowContextUtils.getSeededOperation(context).getAttributes();
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

    private ExecutorResponse userInputRequiredResponse(FlowExecutionContext context) {

        ExecutorResponse response = new ExecutorResponse(STATUS_USER_INPUT_REQUIRED);
        response.setRequiredData(new ArrayList<>(seededAttributes(context)));
        return response;
    }

    private ExecutorResponse errorResponse(String status, StepUpFlowConstants.ErrorMessages error,
                                           String description) {

        ExecutorResponse response = new ExecutorResponse();
        response.setResult(status);
        response.setErrorCode(error.getCode());
        response.setErrorMessage(error.getMessage());
        response.setErrorDescription(description);
        return response;
    }
}
