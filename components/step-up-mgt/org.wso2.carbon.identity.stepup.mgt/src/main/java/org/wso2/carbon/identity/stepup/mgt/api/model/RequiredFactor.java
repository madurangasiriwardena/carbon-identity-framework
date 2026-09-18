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

import org.wso2.carbon.identity.stepup.mgt.api.constant.StepUpConstants;

/**
 * A factor declared by a step-up rule, with the freshness window that applies to it.
 * <p>
 * {@code maxAge} is per factor, not a single value per rule, so a rule may require one factor to have been performed
 * within the hour and another to be performed in the current ceremony.
 */
public final class RequiredFactor {

    private final String factorId;
    private final long maxAge;

    /**
     * Creates a required factor.
     *
     * @param factorId Factor identifier. Must match {@link StepUpConstants#FACTOR_ID_PATTERN}.
     * @param maxAge   Freshness window in seconds. Zero means the factor must be performed in the current ceremony.
     * @throws IllegalArgumentException If the factor identifier is invalid or maxAge is negative.
     */
    public RequiredFactor(String factorId, long maxAge) {

        if (factorId == null || !StepUpConstants.FACTOR_ID_PATTERN.matcher(factorId).matches()) {
            throw new IllegalArgumentException("Invalid factor identifier: " + factorId);
        }
        if (maxAge < 0) {
            throw new IllegalArgumentException("maxAge cannot be negative: " + maxAge);
        }
        this.factorId = factorId;
        this.maxAge = maxAge;
    }

    public String getFactorId() {

        return factorId;
    }

    public long getMaxAge() {

        return maxAge;
    }

    @Override
    public String toString() {

        return "RequiredFactor{factorId='" + factorId + "', maxAge=" + maxAge + '}';
    }
}
