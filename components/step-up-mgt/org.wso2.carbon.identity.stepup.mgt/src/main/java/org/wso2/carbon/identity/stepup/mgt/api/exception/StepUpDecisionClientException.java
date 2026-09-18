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

package org.wso2.carbon.identity.stepup.mgt.api.exception;

/**
 * Step-Up Decision Client Exception.
 * Raised when the step-up policy of a tenant is not valid. A policy that cannot be understood is a configuration
 * error and is never treated as a permissive policy.
 */
public class StepUpDecisionClientException extends StepUpDecisionException {

    private static final long serialVersionUID = -8827345291230948271L;

    public StepUpDecisionClientException(String errorCode, String message) {

        super(errorCode, message);
    }

    public StepUpDecisionClientException(String errorCode, String message, Throwable cause) {

        super(errorCode, message, cause);
    }
}
