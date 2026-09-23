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

package org.wso2.carbon.identity.stepup.flow.constant;

/**
 * Constants of the secured self-service operations flow.
 */
public class StepUpFlowConstants {

    /**
     * Flow type this component's executors belong to.
     */
    public static final String FLOW_TYPE = "SECURED_SELF_SERVICE_OPERATIONS";

    public static final String OPERATION_BRANCH_EXECUTOR = "OperationBranchExecutor";
    public static final String STEP_UP_DECISION_EXECUTOR = "StepUpDecisionExecutor";
    public static final String SECURED_OPERATION_EXECUTOR = "SecuredOperationExecutor";
    public static final String SECURED_OPERATION_COMPLETION_EXECUTOR = "SecuredOperationCompletionExecutor";

    /**
     * Separator of the multi valued context properties written by this component.
     */
    public static final String VALUE_SEPARATOR = ",";

    private StepUpFlowConstants() {

    }

    /**
     * Keys this component reads from the executor configuration of a node in the flow definition.
     * <p>
     * These are authored by whoever writes the flow, not supplied by a caller, and they describe the shape of the
     * flow rather than anything about the request.
     */
    public static class ExecutorMetadata {

        /**
         * Attributes the step this executor sits on may write, separated by {@link #VALUE_SEPARATOR}.
         * <p>
         * Read from the node rather than from the flow context on purpose: the executor that writes an attribute
         * sits in the branch that collects it, so the branch already says which attribute it is. Nothing has to
         * carry it from one step to another, and so nothing can change it in between.
         */
        public static final String OPERATION_ATTRIBUTES = "operationAttributes";

        /**
         * Prefix of the branch to action mappings read by the operation branch executor. The remainder of the key
         * is a branch identifier and the value is the identifier of the action that reaches it.
         */
        public static final String BRANCH_ACTION_PREFIX = "branch.";

        private ExecutorMetadata() {

        }
    }

    /**
     * Keys this component reads from and writes to the flow execution context property map.
     * <p>
     * The property map is server-only: the flow execution API writes request inputs into the user input data, never
     * into the properties. Everything below is therefore evidence the server established, not something a caller
     * can assert.
     * <p>
     * Values are kept primitive on purpose. The flow execution context is persisted as JSON, so a custom type
     * placed in the property map returns as a map after a store round trip, and a long can return as an integer.
     * Strings and lists of strings survive that round trip unchanged.
     */
    public static class ContextProperties {

        private static final String PREFIX = "stepUp.";

        /**
         * ACR restored from the evidence the flow was seeded with. Never set by a factor performed in this flow.
         */
        public static final String INHERITED_ACR = PREFIX + "inheritedAcr";

        /**
         * Authenticators the subject completed before this flow, separated by {@link #VALUE_SEPARATOR}.
         */
        public static final String INHERITED_FACTORS = PREFIX + "inheritedFactors";

        /**
         * Time of the original authentication event, as an ISO-8601 instant. Absent means no known authentication
         * event, which fails every freshness check.
         */
        public static final String INHERITED_AUTH_TIME = PREFIX + "inheritedAuthTime";

        /**
         * Operation the flow was seeded to perform.
         */
        public static final String OPERATION_TYPE = PREFIX + "operationType";

        /**
         * Attributes the seeded operation will touch, separated by {@link #VALUE_SEPARATOR}.
         */
        public static final String OPERATION_ATTRIBUTES = PREFIX + "operationAttributes";

        /**
         * Branch of the flow the seeded operation is performed by. Written once, by whichever operation selection
         * node the flow passed through, and read afterwards to route the collection step without prompting again.
         */
        public static final String OPERATION_BRANCH = PREFIX + "operationBranch";

        /**
         * Factors the seed-time decision found unsatisfied, separated by {@link #VALUE_SEPARATOR}.
         * An empty value means the decision ran and required nothing further.
         */
        public static final String OUTSTANDING_FACTORS = PREFIX + "outstandingFactors";

        /**
         * Marks that the decision has been evaluated. Distinguishes "evaluated, nothing outstanding" from
         * "never evaluated", which must not be treated the same way.
         */
        public static final String DECISION_EVALUATED = PREFIX + "decisionEvaluated";

        /**
         * Callback the flow may terminate on, already validated against the launching application's registered
         * URIs when the flow was seeded. Absent means there is no callback this flow is allowed to redirect to.
         */
        public static final String VALIDATED_CALLBACK_URL = PREFIX + "validatedCallbackUrl";

        private ContextProperties() {

        }
    }

    /**
     * Error messages of the secured self-service operations flow.
     */
    public enum ErrorMessages {

        ERROR_CODE_DECISION_NOT_EVALUATED("65101",
                "Step-up decision was not evaluated.",
                "The operation step was reached without the step-up decision having been evaluated"),
        ERROR_CODE_STEP_UP_NOT_SATISFIED("65102",
                "Required authentication was not performed.",
                "The operation step was reached with the following factors still outstanding: %s"),
        ERROR_CODE_EVALUATING_DECISION("65103",
                "Error while evaluating the step-up decision.",
                "Unexpected error while evaluating the step-up decision for tenant, %s"),
        ERROR_CODE_NO_OPERATION_SEEDED("65104",
                "No operation was seeded for this flow.",
                "The flow was not seeded with an operation, so there is nothing to evaluate or apply"),
        ERROR_CODE_NO_SUBJECT("65105",
                "No subject established for this flow.",
                "The flow carries no user, so the operation cannot be applied"),
        ERROR_CODE_APPLYING_OPERATION("65106",
                "Error while applying the update.",
                "Unexpected error while applying the update for user in tenant, %s"),
        ERROR_CODE_NO_VALIDATED_CALLBACK("65107",
                "No validated callback available.",
                "The flow carries no callback that was validated against the launching application"),
        ERROR_CODE_NOTHING_TO_APPLY("65108",
                "No attribute value was provided.",
                "None of the attributes this flow was seeded for carry a new value"),
        ERROR_CODE_NO_FACTOR_PERFORMED("65109",
                "Required authentication was not performed.",
                "The operation step was reached without any authentication having been performed in this flow"),
        ERROR_CODE_NO_ATTRIBUTE_CONFIGURED("65110",
                "Error in the flow configuration.",
                "The operation step declares no attribute that it may write"),
        ERROR_CODE_NO_ACTION_FOR_BRANCH("65111",
                "Error in the flow configuration.",
                "No action is configured for branch, %s");

        private static final String ERROR_PREFIX = "SSO";
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
