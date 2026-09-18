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

package org.wso2.carbon.identity.stepup.mgt.api.service;

import org.wso2.carbon.identity.stepup.mgt.api.exception.StepUpDecisionException;
import org.wso2.carbon.identity.stepup.mgt.api.model.AuthenticationContextView;
import org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpOperation;

import java.util.List;

/**
 * Decides what additional authentication an operation requires.
 * <p>
 * This is the single place where that question is answered, so that the enforcement points in front of the REST API
 * and the product's own hosted flow can never disagree about which operations are protected.
 */
public interface StepUpDecisionService {

    /**
     * Evaluates what additional authentication is required before the given operation may proceed in the given
     * authentication context.
     * <p>
     * Evaluation fails closed. A factor that is absent from the context, a context with no authentication time, and
     * a factor whose freshness window has elapsed all yield an unsatisfied requirement rather than an unconstrained
     * one. A {@code maxAge} of zero is never satisfied by prior context; the factor must be performed in the current
     * ceremony.
     *
     * @param operation The operation being performed and the attributes it touches.
     * @param context   The authentication context to evaluate against.
     * @return one entry per required factor, each marked satisfied or not. An empty list means no step-up is
     * required.
     * @throws StepUpDecisionException If the tenant's step-up policy cannot be read or is not valid.
     */
    List<FactorRequirement> evaluate(StepUpOperation operation, AuthenticationContextView context)
            throws StepUpDecisionException;
}
