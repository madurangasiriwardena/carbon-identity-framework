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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One rule of a tenant's step-up policy.
 * <p>
 * A rule matches an operation, a set of attributes, or both, and declares the factors required when it matches.
 * A rule that declares neither an operation nor an attribute matches nothing, and a rule that declares no factor
 * requires nothing; both are configuration errors and are rejected when the policy is loaded rather than being
 * treated as permissive.
 */
public final class StepUpRule {

    private final int index;
    private final Set<String> operations;
    private final Set<String> attributes;
    private final List<RequiredFactor> requiredFactors;

    /**
     * Creates a rule.
     *
     * @param index           Position of this rule in the ordered policy.
     * @param operations      Operations this rule matches. Empty means the rule matches on attributes alone.
     * @param attributes      Attributes this rule matches. Empty means the rule matches on operations alone.
     * @param requiredFactors Factors required when this rule matches.
     */
    public StepUpRule(int index, Set<String> operations, Set<String> attributes,
                      List<RequiredFactor> requiredFactors) {

        this.index = index;
        this.operations = Collections.unmodifiableSet(
                operations == null ? new LinkedHashSet<>() : new LinkedHashSet<>(operations));
        this.attributes = Collections.unmodifiableSet(
                attributes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(attributes));
        this.requiredFactors = Collections.unmodifiableList(
                requiredFactors == null ? new ArrayList<>() : new ArrayList<>(requiredFactors));
    }

    public int getIndex() {

        return index;
    }

    public Set<String> getOperations() {

        return operations;
    }

    public Set<String> getAttributes() {

        return attributes;
    }

    public List<RequiredFactor> getRequiredFactors() {

        return requiredFactors;
    }

    /**
     * Whether this rule applies to the given operation.
     * <p>
     * An empty operation set means the rule does not constrain the operation, and an empty attribute set means it
     * does not constrain the attributes. Where both are declared, both must match.
     *
     * @param operation The operation being evaluated.
     * @return true if the rule applies.
     */
    public boolean matches(StepUpOperation operation) {

        if (operation == null) {
            return false;
        }
        if (!operations.isEmpty()
                && (operation.getOperationType() == null || !operations.contains(operation.getOperationType()))) {
            return false;
        }
        if (attributes.isEmpty()) {
            return true;
        }
        return !Collections.disjoint(attributes, operation.getAttributes());
    }

    @Override
    public String toString() {

        return "StepUpRule{index=" + index + ", operations=" + operations + ", attributes=" + attributes
                + ", requiredFactors=" + requiredFactors + '}';
    }
}
