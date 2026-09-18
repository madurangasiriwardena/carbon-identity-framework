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

package org.wso2.carbon.identity.stepup.mgt.internal.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.stepup.mgt.api.constant.StepUpConstants;
import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionException;
import org.wso2.carbon.identity.stepup.mgt.api.model.AuthenticationContextView;
import org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement;
import org.wso2.carbon.identity.stepup.mgt.api.model.RequiredFactor;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpOperation;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpPolicy;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpRule;
import org.wso2.carbon.identity.stepup.mgt.api.service.StepUpDecisionService;
import org.wso2.carbon.identity.stepup.mgt.internal.config.StepUpPolicyProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link StepUpDecisionService}.
 */
public class StepUpDecisionServiceImpl implements StepUpDecisionService {

    private static final Log LOG = LogFactory.getLog(StepUpDecisionServiceImpl.class);

    private final StepUpPolicyProvider policyProvider;
    private final Clock clock;

    public StepUpDecisionServiceImpl() {

        this(new StepUpPolicyProvider(), Clock.systemUTC());
    }

    public StepUpDecisionServiceImpl(StepUpPolicyProvider policyProvider, Clock clock) {

        this.policyProvider = policyProvider;
        this.clock = clock;
    }

    @Override
    public List<FactorRequirement> evaluate(StepUpOperation operation, AuthenticationContextView context)
            throws StepUpDecisionException {

        if (operation == null) {
            return Collections.emptyList();
        }

        StepUpPolicy policy = policyProvider.getPolicy();
        if (policy.isEmpty()) {
            return Collections.emptyList();
        }

        // A flow may touch several attributes at once. The requirement is the union of the requirements of every
        // matching rule, deduplicated by factor, keeping the strictest freshness where a factor is required more
        // than once, so that one ceremony covers the whole operation.
        Map<String, Long> strictestByFactor = new LinkedHashMap<>();
        for (StepUpRule rule : policy.getRules()) {
            if (!rule.matches(operation)) {
                continue;
            }
            for (RequiredFactor factor : rule.getRequiredFactors()) {
                strictestByFactor.merge(factor.getFactorId(), factor.getMaxAge(), Math::min);
            }
        }

        if (strictestByFactor.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No step-up rule matched the operation: " + operation);
            }
            return Collections.emptyList();
        }

        List<FactorRequirement> requirements = new ArrayList<>(strictestByFactor.size());
        for (Map.Entry<String, Long> entry : strictestByFactor.entrySet()) {
            requirements.add(new FactorRequirement(entry.getKey(), entry.getValue(),
                    isSatisfied(entry.getKey(), entry.getValue(), context)));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Step-up decision for operation " + operation + " is " + requirements);
        }
        return Collections.unmodifiableList(requirements);
    }

    /**
     * Whether the given context already satisfies a factor within its freshness window.
     * <p>
     * Every path that cannot be shown to be satisfied returns false. An absent factor, an absent authentication
     * time, an authentication time in the future and an elapsed window are all unsatisfied rather than
     * unconstrained.
     */
    private boolean isSatisfied(String factorId, long maxAge, AuthenticationContextView context) {

        if (context == null) {
            return false;
        }
        // A maxAge of zero is not a window of zero length that a sufficiently recent authentication could fall
        // inside. It means the factor must be performed in the current ceremony, so no prior context satisfies it.
        if (maxAge == StepUpConstants.MAX_AGE_ALWAYS_PROMPT) {
            return false;
        }
        if (!context.hasFactor(factorId)) {
            return false;
        }
        Optional<Instant> authTime = context.getAuthTime();
        if (!authTime.isPresent()) {
            return false;
        }
        Instant now = Instant.now(clock);
        if (authTime.get().isAfter(now)) {
            return false;
        }
        return Duration.between(authTime.get(), now).getSeconds() <= maxAge;
    }
}
