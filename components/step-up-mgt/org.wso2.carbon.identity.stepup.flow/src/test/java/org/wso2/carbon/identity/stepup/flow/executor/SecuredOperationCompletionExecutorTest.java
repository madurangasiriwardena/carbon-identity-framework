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
import org.wso2.carbon.identity.flow.execution.engine.Constants;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;

import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_EXTERNAL_REDIRECTION;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.VALIDATED_CALLBACK_URL;

/**
 * Unit tests of {@link SecuredOperationCompletionExecutor}.
 */
public class SecuredOperationCompletionExecutorTest {

    private SecuredOperationCompletionExecutor executor;
    private FlowExecutionContext context;

    @BeforeMethod
    public void setUp() {

        executor = new SecuredOperationCompletionExecutor();
        context = new FlowExecutionContext();
    }

    @Test(description = "A flow carrying a callback that was validated when it was seeded redirects to it.")
    public void testValidatedCallbackIsRedirectedTo() {

        context.setProperty(VALIDATED_CALLBACK_URL, "myapp://self-care/callback?result=updated");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_EXTERNAL_REDIRECTION);
        assertEquals(response.getAdditionalInfo().get(Constants.REDIRECT_URL),
                "myapp://self-care/callback?result=updated");
    }

    @Test(description = "A flow carrying no validated callback refuses rather than redirecting somewhere that was "
            + "never checked against the launching application.")
    public void testAbsentCallbackIsRefused() {

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(), "SSO-65107");
    }

    @Test(description = "A blank callback is treated as absent.")
    public void testBlankCallbackIsRefused() {

        context.setProperty(VALIDATED_CALLBACK_URL, "   ");

        assertEquals(executor.execute(context).getResult(), STATUS_ERROR);
    }
}
