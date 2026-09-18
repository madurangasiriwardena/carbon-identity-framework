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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Identifies what is being done and which attributes it touches.
 * <p>
 * A caller declares every attribute the operation will touch, not one attribute at a time. The decision is then the
 * union of the requirements of all of them, so that a single ceremony covers the whole operation.
 */
public final class StepUpOperation {

    private final String operationType;
    private final Set<String> attributes;

    private StepUpOperation(Builder builder) {

        this.operationType = builder.operationType;
        this.attributes = Collections.unmodifiableSet(new LinkedHashSet<>(builder.attributes));
    }

    /**
     * The operation being performed, for example a profile update or a password change.
     *
     * @return The operation type. May be null when the operation is identified only by the attributes it touches.
     */
    public String getOperationType() {

        return operationType;
    }

    /**
     * The attributes this operation will touch.
     *
     * @return The attributes. Never null, possibly empty.
     */
    public Set<String> getAttributes() {

        return attributes;
    }

    /**
     * Returns a builder for this operation.
     *
     * @return A new builder.
     */
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Builder of {@link StepUpOperation}.
     */
    public static class Builder {

        private String operationType;
        private Set<String> attributes = new LinkedHashSet<>();

        public Builder operationType(String operationType) {

            this.operationType = operationType;
            return this;
        }

        public Builder attributes(Set<String> attributes) {

            this.attributes = attributes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(attributes);
            return this;
        }

        public Builder addAttribute(String attribute) {

            if (attribute != null) {
                this.attributes.add(attribute);
            }
            return this;
        }

        public StepUpOperation build() {

            return new StepUpOperation(this);
        }
    }

    @Override
    public String toString() {

        return "StepUpOperation{operationType='" + operationType + "', attributes=" + attributes + '}';
    }
}
