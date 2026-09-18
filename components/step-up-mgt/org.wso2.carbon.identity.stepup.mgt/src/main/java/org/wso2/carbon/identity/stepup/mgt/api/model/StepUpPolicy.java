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

package org.wso2.carbon.identity.stepup.mgt.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ordered step-up rule list of a tenant.
 * <p>
 * A tenant with no step-up policy configured has an empty policy, which requires nothing of any operation.
 */
public final class StepUpPolicy {

    private final List<StepUpRule> rules;

    public StepUpPolicy(List<StepUpRule> rules) {

        this.rules = Collections.unmodifiableList(rules == null ? new ArrayList<>() : new ArrayList<>(rules));
    }

    /**
     * The policy of a tenant that has none configured.
     *
     * @return An empty policy.
     */
    public static StepUpPolicy empty() {

        return new StepUpPolicy(Collections.emptyList());
    }

    /**
     * The rules of this policy, in configured order.
     *
     * @return The rules. Never null, possibly empty.
     */
    public List<StepUpRule> getRules() {

        return rules;
    }

    /**
     * Whether this policy carries no rules.
     *
     * @return true if the policy is empty.
     */
    public boolean isEmpty() {

        return rules.isEmpty();
    }

    @Override
    public String toString() {

        return "StepUpPolicy{rules=" + rules + '}';
    }
}
