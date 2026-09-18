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

package org.wso2.carbon.identity.stepup.mgt.internal.config;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.stepup.mgt.api.constant.StepUpConstants;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionClientException;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionException;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionServerException;
import org.wso2.carbon.identity.stepup.mgt.api.model.RequiredFactor;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpPolicy;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpRule;
import org.wso2.carbon.identity.stepup.mgt.internal.component.StepUpMgtComponentServiceHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Reads and validates the step-up policy of a tenant from the configuration store.
 * <p>
 * The whole policy is validated when it is read. A policy that cannot be understood is reported as a configuration
 * error; it is never partially applied and never degrades into a policy that requires nothing.
 * <p>
 * The policy is held in a single configuration resource per tenant. A single resource is read through the
 * configuration store's by-name cache, whereas a resource per rule would have to be read with
 * {@code getResourcesByType}, which is not cached, and would give up atomic validation of the policy as a whole.
 */
public class StepUpPolicyProvider {

    private static final Log LOG = LogFactory.getLog(StepUpPolicyProvider.class);

    /**
     * Reads the step-up policy of the tenant in the current carbon context.
     *
     * @return The policy, or an empty policy when the tenant has none configured.
     * @throws StepUpDecisionException If the policy cannot be read, or is not valid.
     */
    public StepUpPolicy getPolicy() throws StepUpDecisionException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        Resource resource = getResource(tenantDomain);
        if (resource == null || resource.getAttributes() == null || resource.getAttributes().isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No step-up policy configured for tenant: " + tenantDomain);
            }
            return StepUpPolicy.empty();
        }
        return buildPolicy(resource.getAttributes(), tenantDomain);
    }

    private Resource getResource(String tenantDomain) throws StepUpDecisionServerException {

        try {
            return StepUpMgtComponentServiceHolder.getInstance().getConfigurationManager()
                    .getResource(StepUpConstants.STEP_UP_POLICY_RESOURCE_TYPE,
                            StepUpConstants.STEP_UP_POLICY_RESOURCE_NAME, true);
        } catch (ConfigurationManagementException e) {
            if (ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode()
                    .equals(e.getErrorCode())
                    || ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode()
                    .equals(e.getErrorCode())) {
                return null;
            }
            throw new StepUpDecisionServerException(
                    StepUpConstants.ErrorMessages.ERROR_CODE_RETRIEVING_POLICY.getCode(),
                    String.format(StepUpConstants.ErrorMessages.ERROR_CODE_RETRIEVING_POLICY.getDescription(),
                            tenantDomain), e);
        }
    }

    private StepUpPolicy buildPolicy(List<Attribute> attributes, String tenantDomain)
            throws StepUpDecisionClientException {

        Map<Integer, RuleDraft> drafts = new TreeMap<>();
        for (Attribute attribute : attributes) {
            if (attribute == null || StringUtils.isBlank(attribute.getKey())) {
                continue;
            }
            collectAttribute(attribute, drafts, tenantDomain);
        }

        List<StepUpRule> rules = new ArrayList<>(drafts.size());
        for (Map.Entry<Integer, RuleDraft> entry : drafts.entrySet()) {
            rules.add(entry.getValue().toRule(entry.getKey(), tenantDomain));
        }
        return new StepUpPolicy(rules);
    }

    private void collectAttribute(Attribute attribute, Map<Integer, RuleDraft> drafts, String tenantDomain)
            throws StepUpDecisionClientException {

        String key = attribute.getKey().trim();

        Matcher matcherKey = StepUpConstants.RULE_MATCHER_KEY_PATTERN.matcher(key);
        if (matcherKey.matches()) {
            RuleDraft draft = draftFor(drafts, Integer.parseInt(matcherKey.group(1)));
            if (StepUpConstants.PolicyAttributeKeys.OPERATIONS.equals(matcherKey.group(2))) {
                draft.operations = splitList(attribute.getValue());
            } else {
                draft.attributes = splitList(attribute.getValue());
            }
            return;
        }

        Matcher factorKey = StepUpConstants.RULE_FACTOR_KEY_PATTERN.matcher(key);
        if (factorKey.matches()) {
            RuleDraft draft = draftFor(drafts, Integer.parseInt(factorKey.group(1)));
            FactorDraft factor = draft.factors.computeIfAbsent(Integer.parseInt(factorKey.group(2)),
                    index -> new FactorDraft());
            if (StepUpConstants.PolicyAttributeKeys.FACTOR_ID.equals(factorKey.group(3))) {
                factor.factorId = StringUtils.trimToNull(attribute.getValue());
            } else {
                factor.maxAge = StringUtils.trimToNull(attribute.getValue());
            }
            return;
        }

        throw new StepUpDecisionClientException(
                StepUpConstants.ErrorMessages.ERROR_CODE_MALFORMED_ATTRIBUTE_KEY.getCode(),
                String.format(StepUpConstants.ErrorMessages.ERROR_CODE_MALFORMED_ATTRIBUTE_KEY.getDescription(),
                        tenantDomain, key));
    }

    private RuleDraft draftFor(Map<Integer, RuleDraft> drafts, int index) {

        return drafts.computeIfAbsent(index, key -> new RuleDraft());
    }

    private static Set<String> splitList(String value) {

        if (StringUtils.isBlank(value)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(value.split(StepUpConstants.LIST_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Accumulates the attributes of one rule while the resource is read, before the rule is validated.
     */
    private static class RuleDraft {

        private Set<String> operations = new LinkedHashSet<>();
        private Set<String> attributes = new LinkedHashSet<>();
        private final Map<Integer, FactorDraft> factors = new TreeMap<>();

        private StepUpRule toRule(int index, String tenantDomain) throws StepUpDecisionClientException {

            if (operations.isEmpty() && attributes.isEmpty()) {
                throw new StepUpDecisionClientException(
                        StepUpConstants.ErrorMessages.ERROR_CODE_RULE_WITHOUT_MATCHER.getCode(),
                        String.format(StepUpConstants.ErrorMessages.ERROR_CODE_RULE_WITHOUT_MATCHER.getDescription(),
                                index, tenantDomain));
            }
            if (factors.isEmpty()) {
                throw new StepUpDecisionClientException(
                        StepUpConstants.ErrorMessages.ERROR_CODE_RULE_WITHOUT_FACTOR.getCode(),
                        String.format(StepUpConstants.ErrorMessages.ERROR_CODE_RULE_WITHOUT_FACTOR.getDescription(),
                                index, tenantDomain));
            }

            List<RequiredFactor> requiredFactors = new ArrayList<>(factors.size());
            Set<String> seen = new LinkedHashSet<>();
            for (Map.Entry<Integer, FactorDraft> entry : factors.entrySet()) {
                RequiredFactor factor = entry.getValue().toRequiredFactor(entry.getKey(), index, tenantDomain);
                if (!seen.add(factor.getFactorId())) {
                    throw new StepUpDecisionClientException(
                            StepUpConstants.ErrorMessages.ERROR_CODE_DUPLICATE_FACTOR_IN_RULE.getCode(),
                            String.format(
                                    StepUpConstants.ErrorMessages.ERROR_CODE_DUPLICATE_FACTOR_IN_RULE.getDescription(),
                                    factor.getFactorId(), index, tenantDomain));
                }
                requiredFactors.add(factor);
            }
            return new StepUpRule(index, operations, attributes, requiredFactors);
        }
    }

    /**
     * Accumulates the attributes of one factor of one rule while the resource is read.
     */
    private static class FactorDraft {

        private String factorId;
        private String maxAge;

        private RequiredFactor toRequiredFactor(int factorIndex, int ruleIndex, String tenantDomain)
                throws StepUpDecisionClientException {

            if (factorId == null || maxAge == null) {
                throw new StepUpDecisionClientException(
                        StepUpConstants.ErrorMessages.ERROR_CODE_INCOMPLETE_FACTOR.getCode(),
                        String.format(StepUpConstants.ErrorMessages.ERROR_CODE_INCOMPLETE_FACTOR.getDescription(),
                                factorIndex, ruleIndex, tenantDomain));
            }
            if (!StepUpConstants.FACTOR_ID_PATTERN.matcher(factorId).matches()) {
                throw new StepUpDecisionClientException(
                        StepUpConstants.ErrorMessages.ERROR_CODE_INVALID_FACTOR_ID.getCode(),
                        String.format(StepUpConstants.ErrorMessages.ERROR_CODE_INVALID_FACTOR_ID.getDescription(),
                                factorId, ruleIndex, tenantDomain));
            }
            long parsedMaxAge;
            try {
                parsedMaxAge = Long.parseLong(maxAge);
            } catch (NumberFormatException e) {
                throw new StepUpDecisionClientException(
                        StepUpConstants.ErrorMessages.ERROR_CODE_INVALID_MAX_AGE.getCode(),
                        String.format(StepUpConstants.ErrorMessages.ERROR_CODE_INVALID_MAX_AGE.getDescription(),
                                maxAge, factorId, ruleIndex, tenantDomain), e);
            }
            if (parsedMaxAge < 0) {
                throw new StepUpDecisionClientException(
                        StepUpConstants.ErrorMessages.ERROR_CODE_NEGATIVE_MAX_AGE.getCode(),
                        String.format(StepUpConstants.ErrorMessages.ERROR_CODE_NEGATIVE_MAX_AGE.getDescription(),
                                parsedMaxAge, factorId, ruleIndex, tenantDomain));
            }
            return new RequiredFactor(factorId, parsedMaxAge);
        }
    }
}
