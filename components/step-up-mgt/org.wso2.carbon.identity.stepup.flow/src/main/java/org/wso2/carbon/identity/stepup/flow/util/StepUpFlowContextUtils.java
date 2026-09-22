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

package org.wso2.carbon.identity.stepup.flow.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.flow.execution.engine.graph.AuthenticationExecutor;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;
import org.wso2.carbon.identity.stepup.flow.internal.component.StepUpFlowComponentServiceHolder;
import org.wso2.carbon.identity.stepup.mgt.api.model.AuthenticationContextView;
import org.wso2.carbon.identity.stepup.mgt.api.model.StepUpOperation;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.DECISION_EVALUATED;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.INHERITED_ACR;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.INHERITED_AUTH_TIME;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.INHERITED_FACTORS;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_ATTRIBUTES;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_BRANCH;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OPERATION_TYPE;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.OUTSTANDING_FACTORS;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.ContextProperties.VALIDATED_CALLBACK_URL;
import static org.wso2.carbon.identity.stepup.flow.constant.StepUpFlowConstants.VALUE_SEPARATOR;

/**
 * Reads and writes the step-up state this component keeps on the flow execution context.
 * <p>
 * Every read defends against the types a value can come back as. The flow execution context is persisted as JSON,
 * so a value written as a {@code List} can return as a {@code List}, a value written as a {@code String} returns as
 * a {@code String}, and nothing may be assumed to have survived as a richer type than that.
 */
public class StepUpFlowContextUtils {

    private static final Log LOG = LogFactory.getLog(StepUpFlowContextUtils.class);

    private StepUpFlowContextUtils() {

    }

    /**
     * Builds the view of prior authentication this flow was seeded with.
     * <p>
     * Only inherited evidence is carried. A factor performed inside this flow is deliberately not folded in here:
     * doing so would stamp the current time onto evidence that is in fact old, which is the confusion the seed-time
     * decision exists to avoid.
     *
     * @param context Flow execution context.
     * @return The inherited authentication context view.
     */
    public static AuthenticationContextView getInheritedEvidence(FlowExecutionContext context) {

        AuthenticationContextView.Builder builder = AuthenticationContextView.builder()
                .achievedAcr(readString(context, INHERITED_ACR))
                .achievedFactors(new LinkedHashSet<>(readList(context, INHERITED_FACTORS)));
        if (context != null && context.getFlowUser() != null) {
            builder.subjectId(context.getFlowUser().getUserId());
        }
        String authTime = readString(context, INHERITED_AUTH_TIME);
        if (StringUtils.isNotBlank(authTime)) {
            try {
                builder.authTime(Instant.parse(authTime));
            } catch (DateTimeParseException e) {
                // An unreadable authentication time is no known authentication event. Leaving it absent fails every
                // freshness check, which is the safe reading.
                LOG.warn("Ignoring unparseable inherited authentication time on the flow context.");
            }
        }
        return builder.build();
    }

    /**
     * Builds the operation this flow was seeded to perform.
     *
     * @param context Flow execution context.
     * @return The operation, or null when the flow carries no seeded operation.
     */
    public static StepUpOperation getSeededOperation(FlowExecutionContext context) {

        String operationType = readString(context, OPERATION_TYPE);
        List<String> attributes = readList(context, OPERATION_ATTRIBUTES);
        if (StringUtils.isBlank(operationType) && attributes.isEmpty()) {
            return null;
        }
        return StepUpOperation.builder()
                .operationType(operationType)
                .attributes(new LinkedHashSet<>(attributes))
                .build();
    }

    /**
     * The branch of the flow the seeded operation is performed by.
     *
     * @param context Flow execution context.
     * @return The branch identifier, or null when no branch has been selected.
     */
    public static String getOperationBranch(FlowExecutionContext context) {

        return readString(context, OPERATION_BRANCH);
    }

    /**
     * Reads a value from the executor configuration of the node currently being executed.
     * <p>
     * This is flow definition data, authored by whoever wrote the flow. It is not request input and carries none of
     * the doubt that request input does.
     *
     * @param context Flow execution context.
     * @param key     Metadata key.
     * @return The configured value, or null when the node declares none.
     */
    public static String getExecutorMetadata(FlowExecutionContext context, String key) {

        Map<String, String> metadata = getExecutorMetadata(context);
        return metadata == null ? null : StringUtils.trimToNull(metadata.get(key));
    }

    /**
     * Reads the whole executor configuration of the node currently being executed.
     *
     * @param context Flow execution context.
     * @return The configured metadata. Never null.
     */
    public static Map<String, String> getExecutorMetadata(FlowExecutionContext context) {

        if (context == null || context.getCurrentNode() == null
                || context.getCurrentNode().getExecutorConfig() == null
                || context.getCurrentNode().getExecutorConfig().getMetadata() == null) {
            return Collections.emptyMap();
        }
        return context.getCurrentNode().getExecutorConfig().getMetadata();
    }

    /**
     * Splits a stored multi valued property or a configured multi valued metadata value.
     *
     * @param value Value to split.
     * @return The separated values. Never null.
     */
    public static List<String> splitValues(String value) {

        if (StringUtils.isBlank(value)) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(VALUE_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    /**
     * Whether the step-up decision has been evaluated for this flow.
     * <p>
     * A flow where the decision never ran is not the same as a flow where it ran and required nothing, and the two
     * must never collapse into each other.
     *
     * @param context Flow execution context.
     * @return true if the decision has been evaluated.
     */
    public static boolean isDecisionEvaluated(FlowExecutionContext context) {

        Object value = readProperty(context, DECISION_EVALUATED);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    /**
     * Factors the seed-time decision found unsatisfied.
     *
     * @param context Flow execution context.
     * @return The outstanding factor identifiers. Never null.
     */
    public static List<String> getOutstandingFactors(FlowExecutionContext context) {

        return readList(context, OUTSTANDING_FACTORS);
    }

    /**
     * The callback this flow is allowed to terminate on.
     *
     * @param context Flow execution context.
     * @return The validated callback, or null when the flow carries none.
     */
    public static String getValidatedCallbackUrl(FlowExecutionContext context) {

        return readString(context, VALIDATED_CALLBACK_URL);
    }

    /**
     * The authenticators the subject completed inside this flow.
     * <p>
     * Derived from the nodes the engine has marked complete, resolved through the executors registered with this
     * component. Only an {@link AuthenticationExecutor} contributes: a node that restores evidence or applies an
     * update is not a factor the user performed.
     *
     * @param context Flow execution context.
     * @return Authenticator names completed in this flow. Never null.
     */
    public static Set<String> getFactorsCompletedInFlow(FlowExecutionContext context) {

        if (context == null || context.getCompletedNodes() == null) {
            return Collections.emptySet();
        }
        Map<String, Executor> executors = StepUpFlowComponentServiceHolder.getInstance().getExecutors();
        Set<String> factors = new LinkedHashSet<>();
        for (NodeConfig node : context.getCompletedNodes()) {
            if (node == null || node.getExecutorConfig() == null) {
                continue;
            }
            Executor executor = executors.get(node.getExecutorConfig().getName());
            if (executor instanceof AuthenticationExecutor) {
                String amrValue = ((AuthenticationExecutor) executor).getAMRValue();
                if (StringUtils.isNotBlank(amrValue)) {
                    factors.add(amrValue);
                }
            }
        }
        return factors;
    }

    /**
     * Joins a collection into the separated form this component stores multi valued properties in.
     *
     * @param values Values to join.
     * @return The joined value.
     */
    public static String joinValues(Collection<String> values) {

        if (values == null || values.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return values.stream().filter(StringUtils::isNotBlank).collect(Collectors.joining(VALUE_SEPARATOR));
    }

    private static Object readProperty(FlowExecutionContext context, String key) {

        if (context == null || context.getProperties() == null) {
            return null;
        }
        return context.getProperty(key);
    }

    private static String readString(FlowExecutionContext context, String key) {

        Object value = readProperty(context, key);
        return value == null ? null : StringUtils.trimToNull(value.toString());
    }

    @SuppressWarnings("unchecked")
    private static List<String> readList(FlowExecutionContext context, String key) {

        Object value = readProperty(context, key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof Collection) {
            return ((Collection<Object>) value).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        }
        return splitValues(value.toString());
    }
}
