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
 * One factor that is required before an operation may proceed, and whether the evaluated context satisfies it.
 */
public final class FactorRequirement {

    private final String factorId;
    private final long maxAge;
    private final boolean satisfied;

    /**
     * Creates a factor requirement.
     *
     * @param factorId  Factor identifier. Must match {@link StepUpConstants#FACTOR_ID_PATTERN}.
     * @param maxAge    Freshness window in seconds. Zero means the factor must be performed in the current ceremony.
     * @param satisfied Whether the evaluated context satisfies this requirement.
     * @throws IllegalArgumentException If the factor identifier is null, empty or carries a character outside the
     *                                  permitted set, or if maxAge is negative.
     */
    public FactorRequirement(String factorId, long maxAge, boolean satisfied) {

        if (factorId == null || !StepUpConstants.FACTOR_ID_PATTERN.matcher(factorId).matches()) {
            throw new IllegalArgumentException("Invalid factor identifier: " + factorId);
        }
        if (maxAge < 0) {
            throw new IllegalArgumentException("maxAge cannot be negative: " + maxAge);
        }
        this.factorId = factorId;
        this.maxAge = maxAge;
        this.satisfied = satisfied;
    }

    /**
     * The required factor.
     *
     * @return The factor identifier.
     */
    public String getFactorId() {

        return factorId;
    }

    /**
     * Freshness window of this factor, in seconds.
     *
     * @return The maxAge. Zero means no prior authentication can satisfy it.
     */
    public long getMaxAge() {

        return maxAge;
    }

    /**
     * Whether the evaluated context already satisfies this requirement.
     *
     * @return true if satisfied.
     */
    public boolean isSatisfied() {

        return satisfied;
    }

    @Override
    public String toString() {

        return "FactorRequirement{factorId='" + factorId + "', maxAge=" + maxAge + ", satisfied=" + satisfied + '}';
    }
}
