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

package org.wso2.carbon.identity.stepup.mgt.config;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementClientException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementServerException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionClientException;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionServerException;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpPolicy;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpRule;
import org.wso2.carbon.identity.stepup.mgt.internal.component.StepUpMgtComponentServiceHolder;
import org.wso2.carbon.identity.stepup.mgt.internal.config.StepUpPolicyProvider;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests of {@link StepUpPolicyProvider}.
 * <p>
 * Every case that rejects a policy is a case where the alternative would be to run with a policy that quietly
 * requires less than the administrator wrote down.
 */
public class StepUpPolicyProviderTest {

    private static final String TENANT_DOMAIN = "carbon.super";
    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String EMAIL_OTP = "email-otp-authenticator";
    private static final String SMS_OTP = "sms-otp-authenticator";

    private MockedStatic<PrivilegedCarbonContext> carbonContext;
    private ConfigurationManager configurationManager;
    private StepUpPolicyProvider policyProvider;

    @BeforeMethod
    public void setUp() {

        System.setProperty(CarbonBaseConstants.CARBON_HOME,
                Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString());

        carbonContext = mockStatic(PrivilegedCarbonContext.class);
        PrivilegedCarbonContext threadLocalContext = mock(PrivilegedCarbonContext.class);
        when(threadLocalContext.getTenantDomain()).thenReturn(TENANT_DOMAIN);
        carbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(threadLocalContext);

        configurationManager = mock(ConfigurationManager.class);
        StepUpMgtComponentServiceHolder.getInstance().setConfigurationManager(configurationManager);
        policyProvider = new StepUpPolicyProvider();
    }

    @AfterMethod
    public void tearDown() {

        carbonContext.close();
        StepUpMgtComponentServiceHolder.getInstance().setConfigurationManager(null);
    }

    @Test(description = "A tenant that has never configured a policy reads as an empty policy.")
    public void testAbsentResourceYieldsEmptyPolicy() throws Exception {

        givenConfigurationStoreThrows(new ConfigurationManagementClientException("no resource",
                ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode()));

        assertTrue(policyProvider.getPolicy().isEmpty());
    }

    @Test(description = "A deployment where the resource type was never created reads as an empty policy.")
    public void testAbsentResourceTypeYieldsEmptyPolicy() throws Exception {

        givenConfigurationStoreThrows(new ConfigurationManagementClientException("no resource type",
                ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode()));

        assertTrue(policyProvider.getPolicy().isEmpty());
    }

    @Test(description = "A resource carrying no attributes reads as an empty policy.")
    public void testResourceWithoutAttributesYieldsEmptyPolicy() throws Exception {

        givenResource(new ArrayList<>());

        assertTrue(policyProvider.getPolicy().isEmpty());
    }

    @Test(description = "A failure to read the policy is a server error, never an empty policy.")
    public void testReadFailureIsReportedRatherThanTreatedAsNoPolicy() throws Exception {

        givenConfigurationStoreThrows(new ConfigurationManagementServerException("db down", "CONFIGM_00099"));

        try {
            policyProvider.getPolicy();
            throw new AssertionError("Expected a StepUpDecisionServerException.");
        } catch (StepUpDecisionServerException e) {
            assertEquals(e.getErrorCode(), "SUP-60001");
        }
    }

    @Test(description = "A well formed policy parses into ordered rules with per factor freshness.")
    public void testValidPolicyIsParsed() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM + ",http://wso2.org/claims/mobile"),
                new Attribute("rule.0.factor.0.id", EMAIL_OTP),
                new Attribute("rule.0.factor.0.maxAge", "0"),
                new Attribute("rule.1.operations", "UPDATE_PASSWORD"),
                new Attribute("rule.1.factor.0.id", SMS_OTP),
                new Attribute("rule.1.factor.0.maxAge", "300")));

        StepUpPolicy policy = policyProvider.getPolicy();

        assertEquals(policy.getRules().size(), 2);
        StepUpRule first = policy.getRules().get(0);
        assertEquals(first.getIndex(), 0);
        assertEquals(first.getAttributes().size(), 2);
        assertTrue(first.getOperations().isEmpty());
        assertEquals(first.getRequiredFactors().size(), 1);
        assertEquals(first.getRequiredFactors().get(0).getFactorId(), EMAIL_OTP);
        assertEquals(first.getRequiredFactors().get(0).getMaxAge(), 0L);

        StepUpRule second = policy.getRules().get(1);
        assertEquals(second.getOperations(), Collections.singleton("UPDATE_PASSWORD"));
        assertEquals(second.getRequiredFactors().get(0).getMaxAge(), 300L);
    }

    @Test(description = "A rule constraining neither an operation nor an attribute is a configuration error.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testRuleWithoutMatcherIsRejected() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.factor.0.id", EMAIL_OTP),
                new Attribute("rule.0.factor.0.maxAge", "0")));

        policyProvider.getPolicy();
    }

    @Test(description = "A rule requiring no factor is a configuration error, not a permissive rule.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testRuleWithoutFactorIsRejected() throws Exception {

        givenResource(Collections.singletonList(new Attribute("rule.0.attributes", EMAIL_CLAIM)));

        policyProvider.getPolicy();
    }

    @Test(description = "A factor identifier carrying a quote is rejected when the policy is loaded, which is what "
            + "makes emitting it unescaped into the challenge safe.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testFactorIdentifierWithQuoteIsRejectedAtLoad() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM),
                new Attribute("rule.0.factor.0.id", "otp\", max_age=0, injected=\"yes"),
                new Attribute("rule.0.factor.0.maxAge", "0")));

        policyProvider.getPolicy();
    }

    @Test(description = "A factor identifier carrying a control character is rejected when the policy is loaded.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testFactorIdentifierWithControlCharacterIsRejectedAtLoad() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM),
                new Attribute("rule.0.factor.0.id", "otp\r\nX-Injected: yes"),
                new Attribute("rule.0.factor.0.maxAge", "0")));

        policyProvider.getPolicy();
    }

    @Test(description = "A negative freshness window is a configuration error.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testNegativeMaxAgeIsRejected() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM),
                new Attribute("rule.0.factor.0.id", EMAIL_OTP),
                new Attribute("rule.0.factor.0.maxAge", "-1")));

        policyProvider.getPolicy();
    }

    @Test(description = "A freshness window that is not a number is a configuration error.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testNonNumericMaxAgeIsRejected() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM),
                new Attribute("rule.0.factor.0.id", EMAIL_OTP),
                new Attribute("rule.0.factor.0.maxAge", "soon")));

        policyProvider.getPolicy();
    }

    @Test(description = "A factor declared without a freshness window is a configuration error.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testIncompleteFactorIsRejected() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM),
                new Attribute("rule.0.factor.0.id", EMAIL_OTP)));

        policyProvider.getPolicy();
    }

    @Test(description = "The same factor declared twice within one rule is a configuration error.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testDuplicateFactorWithinRuleIsRejected() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM),
                new Attribute("rule.0.factor.0.id", EMAIL_OTP),
                new Attribute("rule.0.factor.0.maxAge", "0"),
                new Attribute("rule.0.factor.1.id", EMAIL_OTP),
                new Attribute("rule.0.factor.1.maxAge", "300")));

        policyProvider.getPolicy();
    }

    @Test(description = "An attribute key the loader does not recognise is a configuration error rather than "
            + "something silently dropped.",
            expectedExceptions = StepUpDecisionClientException.class)
    public void testMalformedAttributeKeyIsRejected() throws Exception {

        givenResource(Arrays.asList(
                new Attribute("rule.0.attributes", EMAIL_CLAIM),
                new Attribute("rule.0.factor.0.id", EMAIL_OTP),
                new Attribute("rule.0.factor.0.maxAge", "0"),
                new Attribute("rule.0.requiredFactors", EMAIL_OTP)));

        policyProvider.getPolicy();
    }

    private void givenResource(List<Attribute> attributes) throws Exception {

        Resource resource = new Resource();
        resource.setResourceName("step-up-policy");
        resource.setAttributes(attributes);
        when(configurationManager.getResource(anyString(), anyString(), anyBoolean())).thenReturn(resource);
    }

    private void givenConfigurationStoreThrows(Exception exception) throws Exception {

        when(configurationManager.getResource(anyString(), anyString(), anyBoolean())).thenThrow(exception);
    }
}
