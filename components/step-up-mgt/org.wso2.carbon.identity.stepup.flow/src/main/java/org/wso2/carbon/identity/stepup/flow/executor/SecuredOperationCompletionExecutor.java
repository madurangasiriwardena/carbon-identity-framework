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
import org.wso2.carbon.identity.flow.execution.engine.Constants;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.Constants.FlowTypes;
import org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants;
import org.wso2.carbon.identity.stepup.flow.util.StepUpFlowContextUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_EXTERNAL_REDIRECTION;

/**
 * Hands control back to the application that launched the flow.
 * <p>
 * The callback is not taken from the request. It is read from the evidence the flow was seeded with, where it was
 * already checked against the launching application's registered URIs. If the flow carries no such callback, this
 * step refuses rather than redirecting somewhere unverified: an unvalidated redirect out of a context that has just
 * completed an authentication is an open redirect.
 * <p>
 * This executor deliberately does not ask for an authentication assertion to be generated. That is what would turn
 * the flow's outcome into a login, and the user would leave the webview holding a session for every application in
 * the browser.
 */
public class SecuredOperationCompletionExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(SecuredOperationCompletionExecutor.class);

    @Override
    public String getName() {

        return StepUpFlowConstants.SECURED_OPERATION_COMPLETION_EXECUTOR;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        String callbackUrl = StepUpFlowContextUtils.getValidatedCallbackUrl(context);
        if (StringUtils.isBlank(callbackUrl)) {
            LOG.warn("No validated callback available for flow: " + context.getContextIdentifier()
                    + ". Refusing to redirect.");
            ExecutorResponse response = new ExecutorResponse();
            response.setResult(STATUS_ERROR);
            response.setErrorCode(StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_VALIDATED_CALLBACK.getCode());
            response.setErrorMessage(StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_VALIDATED_CALLBACK.getMessage());
            response.setErrorDescription(
                    StepUpFlowConstants.ErrorMessages.ERROR_CODE_NO_VALIDATED_CALLBACK.getDescription());
            return response;
        }

        Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put(Constants.REDIRECT_URL, callbackUrl);

        ExecutorResponse response = new ExecutorResponse(STATUS_EXTERNAL_REDIRECTION);
        response.setAdditionalInfo(additionalInfo);
        return response;
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
}
