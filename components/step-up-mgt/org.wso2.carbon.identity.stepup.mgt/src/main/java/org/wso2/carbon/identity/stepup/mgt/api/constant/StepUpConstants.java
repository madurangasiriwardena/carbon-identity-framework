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

package org.wso2.carbon.identity.stepup.mgt.api.constant;

import java.util.regex.Pattern;

/**
 * Constants for step-up decision evaluation and the step-up policy configuration.
 */
public class StepUpConstants {

    /**
     * Configuration store resource type holding the step-up policy of a tenant.
     */
    public static final String STEP_UP_POLICY_RESOURCE_TYPE = "STEP_UP_POLICY";

    /**
     * Name of the single step-up policy resource of a tenant.
     */
    public static final String STEP_UP_POLICY_RESOURCE_NAME = "step-up-policy";

    /**
     * Separator used within multi valued configuration attributes.
     */
    public static final String LIST_SEPARATOR = ",";

    /**
     * A {@code maxAge} of zero means the factor must be performed in the current ceremony and can never be
     * satisfied by a prior authentication, however recent.
     */
    public static final long MAX_AGE_ALWAYS_PROMPT = 0L;

    /**
     * Matches {@code rule.<ruleIndex>.operations} and {@code rule.<ruleIndex>.attributes} attribute keys.
     */
    public static final Pattern RULE_MATCHER_KEY_PATTERN =
            Pattern.compile("^rule\\.(\\d{1,4})\\.(operations|attributes)$");

    /**
     * Matches {@code rule.<ruleIndex>.factor.<factorIndex>.id} and
     * {@code rule.<ruleIndex>.factor.<factorIndex>.maxAge} attribute keys.
     */
    public static final Pattern RULE_FACTOR_KEY_PATTERN =
            Pattern.compile("^rule\\.(\\d{1,4})\\.factor\\.(\\d{1,4})\\.(id|maxAge)$");

    /**
     * Character set a factor identifier is restricted to.
     * <p>
     * Factor identifiers are echoed verbatim into the quoted-string {@code acr_values} parameter of the RFC 9470
     * {@code WWW-Authenticate} challenge. Restricting them here, at the point a {@link
     * org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement} is constructed, is what makes emitting them
     * without escaping safe. The set excludes the double quote, the backslash, the comma, whitespace and control
     * characters, and admits the characters that appear in authenticator names and in URI shaped ACR values.
     */
    public static final Pattern FACTOR_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9!#$&'()*+\\-./:;=?@\\[\\]_~]{1,255}$");

    private StepUpConstants() {

    }

    /**
     * Attribute key fragments of the step-up policy resource.
     */
    public static class PolicyAttributeKeys {

        public static final String RULE_PREFIX = "rule.";
        public static final String OPERATIONS = "operations";
        public static final String ATTRIBUTES = "attributes";
        public static final String FACTOR_SEGMENT = "factor.";
        public static final String FACTOR_ID = "id";
        public static final String FACTOR_MAX_AGE = "maxAge";

        private PolicyAttributeKeys() {

        }
    }

    /**
     * Constants of the RFC 9470 {@code insufficient_user_authentication} challenge.
     */
    public static class Challenge {

        public static final String SCHEME = "Bearer";
        public static final String ERROR_CODE = "insufficient_user_authentication";
        public static final String DEFAULT_ERROR_DESCRIPTION = "A more recent authentication is required";
        public static final String PARAM_ERROR = "error";
        public static final String PARAM_ERROR_DESCRIPTION = "error_description";
        public static final String PARAM_ACR_VALUES = "acr_values";
        public static final String PARAM_MAX_AGE = "max_age";
        public static final String ACR_VALUES_SEPARATOR = " ";

        private Challenge() {

        }
    }

    /**
     * Error messages of the step-up management component.
     */
    public enum ErrorMessages {

        ERROR_CODE_RETRIEVING_POLICY("60001",
                "Error while retrieving the step-up policy.",
                "Unexpected server error while retrieving the step-up policy of tenant, %s"),
        ERROR_CODE_MALFORMED_ATTRIBUTE_KEY("60002",
                "Malformed step-up policy attribute key.",
                "The step-up policy of tenant %s carries the unrecognised attribute key, %s"),
        ERROR_CODE_RULE_WITHOUT_MATCHER("60003",
                "Step-up rule matches nothing.",
                "Rule %s of the step-up policy of tenant %s declares neither an operation nor an attribute"),
        ERROR_CODE_RULE_WITHOUT_FACTOR("60004",
                "Step-up rule requires no factor.",
                "Rule %s of the step-up policy of tenant %s declares no required factor"),
        ERROR_CODE_INVALID_FACTOR_ID("60005",
                "Invalid factor identifier.",
                "The factor identifier '%s' in rule %s of the step-up policy of tenant %s is empty or contains " +
                        "characters outside the permitted set"),
        ERROR_CODE_INVALID_MAX_AGE("60006",
                "Invalid factor maxAge.",
                "The maxAge '%s' of factor '%s' in rule %s of the step-up policy of tenant %s is not a number"),
        ERROR_CODE_NEGATIVE_MAX_AGE("60007",
                "Negative factor maxAge.",
                "The maxAge %s of factor '%s' in rule %s of the step-up policy of tenant %s is negative"),
        ERROR_CODE_INCOMPLETE_FACTOR("60008",
                "Incomplete factor declaration.",
                "Factor %s of rule %s of the step-up policy of tenant %s declares no identifier or no maxAge"),
        ERROR_CODE_DUPLICATE_FACTOR_IN_RULE("60009",
                "Duplicate factor within a rule.",
                "The factor '%s' is declared more than once in rule %s of the step-up policy of tenant %s");

        private static final String ERROR_PREFIX = "SUP";
        private final String code;
        private final String message;
        private final String description;

        /**
         * Constructor to initialize the error message enum.
         *
         * @param code        Error code.
         * @param message     Error message.
         * @param description Error description.
         */
        ErrorMessages(String code, String message, String description) {

            this.code = ERROR_PREFIX + "-" + code;
            this.message = message;
            this.description = description;
        }

        /**
         * Get the error code.
         *
         * @return Error code.
         */
        public String getCode() {

            return code;
        }

        /**
         * Get the error message.
         *
         * @return Error message.
         */
        public String getMessage() {

            return message;
        }

        /**
         * Get the error description.
         *
         * @return Error description.
         */
        public String getDescription() {

            return description;
        }

        @Override
        public String toString() {

            return code + " : " + message;
        }
    }
}
