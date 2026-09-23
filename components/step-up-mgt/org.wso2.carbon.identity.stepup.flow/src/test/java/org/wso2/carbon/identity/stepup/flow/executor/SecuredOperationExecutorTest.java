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

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;
import org.wso2.carbon.identity.stepup.flow.TestExecutors;
import org.wso2.carbon.identity.stepup.flow.TestGraphs;
import org.wso2.carbon.identity.stepup.flow.internal.component.StepUpFlowComponentServiceHolder;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_ERROR;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_USER_INPUT_REQUIRED;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.DECISION_EVALUATED;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OUTSTANDING_FACTORS;

/**
 * Unit tests of {@link SecuredOperationExecutor}.
 * <p>
 * The refusal cases matter most. This executor is the last point at which a flow that reached the update without
 * the required authentication can be stopped.
 */
public class SecuredOperationExecutorTest {

    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String MOBILE_CLAIM = "http://wso2.org/claims/mobile";
    private static final String EMAIL_OTP = "email-otp-authenticator";
    private static final String USER_ID = "8a2f1c44-0f3e-4c01-9d21-0f3e4c019d21";
    private static final String OTP_EXECUTOR = "TestOTPExecutor";

    private MockedStatic<IdentityTenantUtil> tenantUtil;
    private AbstractUserStoreManager userStoreManager;
    private SecuredOperationExecutor executor;
    private FlowExecutionContext context;

    @BeforeMethod
    public void setUp() throws Exception {

        tenantUtil = mockStatic(IdentityTenantUtil.class);
        tenantUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);

        userStoreManager = mock(AbstractUserStoreManager.class);
        UserRealm userRealm = mock(UserRealm.class);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        RealmService realmService = mock(RealmService.class);
        when(realmService.getTenantUserRealm(anyInt())).thenReturn(userRealm);
        StepUpFlowComponentServiceHolder.getInstance().setRealmService(realmService);

        executor = new SecuredOperationExecutor();
        context = new FlowExecutionContext();
        context.setTenantDomain("carbon.super");
        context.getFlowUser().setUserId(USER_ID);
        givenStepWrites(EMAIL_CLAIM);
    }

    /**
     * Puts the executor on a step whose page collects the given claims, the way a collection step in the flow
     * definition does. The page is the declaration, so there is nothing else to configure.
     */
    private void givenStepWrites(String... claims) {

        context.setGraphConfig(TestGraphs.builder()
                .page("collection_step", "operation_completion", claims)
                .build());
        context.setCurrentNode(new NodeConfig.Builder().id("collection_step").build());
    }

    @AfterMethod
    public void tearDown() {

        tenantUtil.close();
        StepUpFlowComponentServiceHolder.getInstance().setRealmService(null);
    }

    @Test(description = "A flow that reached the update with no authentication performed at all is refused. The "
            + "flow definition puts the authentication ahead of this step, but one authored without it must fail "
            + "closed rather than write.")
    public void testUnauthenticatedFlowIsRefused() throws Exception {

        context.getFlowUser().addUpdatedClaim(EMAIL_CLAIM, "new@example.com");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(), "SSO-65109");
        verify(userStoreManager, never()).setUserClaimValuesWithID(anyString(), any(), any());
    }

    @Test(description = "An authenticator completed in this flow is enough to proceed when no decision step ran.")
    public void testPerformedFactorSatisfiesTheGuard() throws Exception {

        givenFactorPerformedInFlow(false);
        context.getFlowUser().addUpdatedClaim(EMAIL_CLAIM, "new@example.com");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        verify(userStoreManager).setUserClaimValuesWithID(eq(USER_ID), any(), any());
    }

    @Test(description = "A step whose page collects no attribute writes nothing, whatever the user supplied.")
    public void testStepWithNoAttributeWritesNothing() throws Exception {

        givenFactorPerformedInFlow();
        givenStepWrites();
        context.getFlowUser().addUpdatedClaim(EMAIL_CLAIM, "new@example.com");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_USER_INPUT_REQUIRED);
        verify(userStoreManager, never()).setUserClaimValuesWithID(anyString(), any(), any());
    }

    @Test(description = "A flow that reached the update with a required factor still outstanding is refused, even "
            + "though the flow definition led it here.")
    public void testOutstandingFactorIsRefused() throws Exception {

        context.setProperty(DECISION_EVALUATED, Boolean.TRUE);
        context.setProperty(OUTSTANDING_FACTORS, EMAIL_OTP);
        context.getFlowUser().addUpdatedClaim(EMAIL_CLAIM, "new@example.com");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(), "SSO-65102");
        verify(userStoreManager, never()).setUserClaimValuesWithID(anyString(), any(), any());
    }

    @Test(description = "Once the outstanding factor has been performed in this flow, the update is applied.")
    public void testUpdateAppliedOnceFactorPerformed() throws Exception {

        givenFactorPerformedInFlow();
        context.getFlowUser().addUpdatedClaim(EMAIL_CLAIM, "new@example.com");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_COMPLETE);
        verify(userStoreManager).setUserClaimValuesWithID(eq(USER_ID), any(), any());
    }

    @Test(description = "Only the attribute the step declares is written. A value supplied for anything else is "
            + "ignored rather than applied alongside it, so a form carrying extra claims cannot widen the update.")
    public void testUndeclaredAttributeIsNotApplied() throws Exception {

        givenFactorPerformedInFlow();
        context.getFlowUser().addUpdatedClaim(EMAIL_CLAIM, "new@example.com");
        context.getFlowUser().addUpdatedClaim(MOBILE_CLAIM, "+94770000000");

        executor.execute(context);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(userStoreManager).setUserClaimValuesWithID(eq(USER_ID), captor.capture(), any());
        assertEquals(captor.getValue().keySet(), Collections.singleton(EMAIL_CLAIM));
    }

    @Test(description = "Reaching the update step with no value supplied asks for one rather than failing.")
    public void testMissingValueAsksForInput() throws Exception {

        givenFactorPerformedInFlow();

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_USER_INPUT_REQUIRED);
        assertTrue(response.getRequiredData().contains(EMAIL_CLAIM));
        verify(userStoreManager, never()).setUserClaimValuesWithID(anyString(), any(), any());
    }

    @Test(description = "A flow carrying no subject cannot apply anything.")
    public void testMissingSubjectIsRefused() throws Exception {

        givenFactorPerformedInFlow();
        context.getFlowUser().setUserId(null);
        context.getFlowUser().addUpdatedClaim(EMAIL_CLAIM, "new@example.com");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_ERROR);
        assertEquals(response.getErrorCode(), "SSO-65105");
        verify(userStoreManager, never()).setUserClaimValuesWithID(anyString(), any(), any());
    }

    @Test(description = "A claim that was merely loaded onto the flow, such as the address an OTP was sent to, is "
            + "not an update and must not be written back.")
    public void testClaimLoadedButNotUpdatedIsNotApplied() throws Exception {

        givenFactorPerformedInFlow();
        // Loaded, not updated: this is what seeding a flow with the user's current values looks like.
        context.getFlowUser().addClaim(EMAIL_CLAIM, "existing@example.com");

        ExecutorResponse response = executor.execute(context);

        assertEquals(response.getResult(), STATUS_USER_INPUT_REQUIRED);
        verify(userStoreManager, never()).setUserClaimValuesWithID(anyString(), any(), any());
    }

    private void givenFactorPerformedInFlow() {

        givenFactorPerformedInFlow(true);
    }

    private void givenFactorPerformedInFlow(boolean withDecision) {

        if (withDecision) {
            context.setProperty(DECISION_EVALUATED, Boolean.TRUE);
            context.setProperty(OUTSTANDING_FACTORS, EMAIL_OTP);
        }
        StepUpFlowComponentServiceHolder.getInstance().addExecutor(
                new TestExecutors.TestAuthenticationExecutor(OTP_EXECUTOR, EMAIL_OTP));
        context.setCompletedNodes(
                Collections.singletonList(TestExecutors.nodeFor(OTP_EXECUTOR)));
    }
}
