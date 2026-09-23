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
import org.wso2.carbon.identity.flow.execution.engine.metadata.FlowExecutorMetadata;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_USER_INPUT_REQUIRED;

/**
 * Applies the protected update the step it sits on is authored to make.
 * <p>
 * Which attribute that is comes from the node's own configuration. The step sits inside the branch that collects the
 * attribute, so the branch already says which one it is; nothing carries it from an earlier step, and so nothing can
 * change it in between. A value supplied for anything else is ignored rather than written.
 * <p>
 * Before applying anything it refuses unless the user was actually authenticated in this flow. The flow is authored
 * with the authentication ahead of this step, but a flow authored with the steps in the wrong order, or with none at
 * all, must fail closed rather than write.
 */
public class SecuredOperationExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(SecuredOperationExecutor.class);

    @Override
    public String getName() {

        return StepUpFlowConstants.SECURED_OPERATION_EXECUTOR;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        ExecutorResponse refusal = refuseIfNotAuthenticated(context);
        if (refusal != null) {
            return refusal;
        }

        if (context.getFlowUser() == null || StringUtils.isBlank(context.getFlowUser().getUserId())) {
            return errorResponse(STATUS_ERROR, StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_SUBJECT,
                    StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_SUBJECT.getDescription());
        }

        Map<String, String> updates = collectWritableAttributeValues(context);
        if (updates.isEmpty()) {
            /*
             Reached on the way to the collection page, before the user has entered anything: returning
             USER_INPUT_REQUIRED here is what causes that page to be shown. It is also what a step whose form
             collects a different attribute from the one it may write looks like, so both lists are named to tell
             the two apart. Claims updated by an earlier step, such as an OTP marking an address verified, appear
             here too without being anything this operation may write.
             */
            warnIfFormCollectsSomethingElse(context);
            if (LOG.isDebugEnabled()) {
                LOG.debug("No attribute supplied yet for flow " + context.getContextIdentifier()
                        + ", asking for input. Attributes this step may write: "
                        + StepUpFlowContextUtils.joinValues(writableAttributes(context)) + ". Claims updated in this "
                        + "flow so far: " + (context.getFlowUser() == null ? ""
                        : StepUpFlowContextUtils.joinValues(context.getFlowUser().getUpdatedClaimUris())));
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
     * Picks out the values the user supplied for the attributes this step may write, and nothing else.
     * <p>
     * The values are read from the flow user rather than from the raw request inputs. The engine validates inputs
     * before a node runs, and in doing so moves every claim URI keyed input onto the flow user and removes it from
     * the input map, so by the time this executor runs the raw inputs no longer carry them.
     * <p>
     * Only claims the user actually changed in this flow are considered. Claims that were merely loaded onto the
     * flow user, such as the address an OTP was sent to, are not updates and must not be written back.
     */
    private Map<String, String> collectWritableAttributeValues(FlowExecutionContext context) {

        Map<String, String> updates = new LinkedHashMap<>();
        Set<String> writableAttributes = writableAttributes(context);
        if (writableAttributes.isEmpty() || context.getFlowUser() == null) {
            return updates;
        }
        Set<String> updatedClaimUris = context.getFlowUser().getUpdatedClaimUris();
        for (String attribute : writableAttributes) {
            if (!updatedClaimUris.contains(attribute)) {
                continue;
            }
            Object value = context.getFlowUser().getClaim(attribute);
            if (value != null && StringUtils.isNotBlank(value.toString())) {
                updates.put(attribute, value.toString());
            }
        }
        return updates;
    }

    /**
     * Says so when the form that reached this executor collects a claim this step cannot write.
     * <p>
     * That pairing is what a flow author has to get right by hand, and getting it wrong produces no error: the
     * executor keeps asking for input it will never be given, so the page simply reappears on every submit. The
     * symptom looks nothing like the cause, hence this.
     * <p>
     * It stays a warning rather than a refusal because the inputs read here are the ones the engine last published
     * for a step, which on the first pass through this node are still the previous step's. Only a mismatch that
     * mentions claims at all is reported, so the ordinary first pass — arriving from an OTP step that collects no
     * claim — says nothing.
     *
     * @param context Flow execution context.
     */
    private void warnIfFormCollectsSomethingElse(FlowExecutionContext context) {

        if (!LOG.isWarnEnabled() || context.getCurrentStepInputs() == null) {
            return;
        }
        Set<String> declared = new LinkedHashSet<>();
        for (Set<String> inputs : context.getCurrentStepInputs().values()) {
            if (inputs != null) {
                declared.addAll(inputs);
            }
        }
        boolean collectsClaims = declared.stream().anyMatch(input -> input != null && input.contains("/"));
        Set<String> writable = writableAttributes(context);
        if (!collectsClaims || declared.stream().anyMatch(writable::contains)) {
            return;
        }
        LOG.warn("Flow " + context.getContextIdentifier() + " has an operation step that may write "
                + StepUpFlowContextUtils.joinValues(writable) + ", but the form reaching it collects "
                + StepUpFlowContextUtils.joinValues(declared) + ". Nothing will be applied and the step will keep "
                + "asking for input until the two are the same.");
    }

    /**
     * The attributes this step is allowed to write, declared on the node itself.
     * <p>
     * Taken from the node rather than from the flow context because the executor sits in the branch that collects
     * the attribute: the branch already says which attribute it is, so nothing carries it between steps and nothing
     * can change it on the way.
     */
    private Set<String> writableAttributes(FlowExecutionContext context) {

        return new LinkedHashSet<>(StepUpFlowContextUtils.splitValues(
                StepUpFlowContextUtils.getExecutorMetadata(context,
                        StepUpFlowConstants.ExecutorMetadata.OPERATION_ATTRIBUTES)));
    }

    /**
     * Refuses the update unless the flow actually authenticated the user.
     * <p>
     * The flow is authored so that the authentication steps precede this one, but a flow authored with them in the
     * wrong order, or with none at all, must fail closed rather than write. Where a decision step has run, its
     * verdict is honoured; otherwise it is enough that some authenticator completed in this flow.
     */
    private ExecutorResponse refuseIfNotAuthenticated(FlowExecutionContext context) {

        Set<String> performed = StepUpFlowContextUtils.getFactorsCompletedInFlow(context);

        if (StepUpFlowContextUtils.isDecisionEvaluated(context)) {
            List<String> outstanding = new ArrayList<>(StepUpFlowContextUtils.getOutstandingFactors(context));
            outstanding.removeAll(performed);
            if (!outstanding.isEmpty()) {
                LOG.warn("Refusing the secured operation for flow " + context.getContextIdentifier()
                        + ": required factors were not performed.");
                return errorResponse(STATUS_ERROR,
                        StepUpFlowConstants.ErrorMessages.ERROR_CODE_STEP_UP_NOT_SATISFIED,
                        String.format(
                                StepUpFlowConstants.ErrorMessages.ERROR_CODE_STEP_UP_NOT_SATISFIED.getDescription(),
                                StepUpFlowContextUtils.joinValues(outstanding)));
            }
            return null;
        }

        if (performed.isEmpty()) {
            LOG.warn("Refusing the secured operation for flow " + context.getContextIdentifier()
                    + ": no authentication was performed in this flow.");
            return errorResponse(STATUS_ERROR, StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_FACTOR_PERFORMED,
                    StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_FACTOR_PERFORMED.getDescription());
        }
        return null;
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
                .displayName("Update the protected attribute")
                .description("Applies the attribute this step declares, once the user has been authenticated in this "
                        + "flow.")
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

    private ExecutorResponse userInputRequiredResponse(FlowExecutionContext context) {

        ExecutorResponse response = new ExecutorResponse(STATUS_USER_INPUT_REQUIRED);
        response.setRequiredData(new ArrayList<>(writableAttributes(context)));
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
