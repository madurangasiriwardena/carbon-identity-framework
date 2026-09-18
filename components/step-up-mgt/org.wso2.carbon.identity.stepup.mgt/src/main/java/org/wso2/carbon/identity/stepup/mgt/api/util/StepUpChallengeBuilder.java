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

package org.wso2.carbon.identity.stepup.mgt.api.util;

import org.wso2.carbon.identity.stepup.mgt.api.constant.StepUpConstants;
import org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds the RFC 9470 {@code insufficient_user_authentication} challenge for a decision result.
 * <p>
 * This lives beside the decision component rather than in the REST enforcement point so that every caller emits one
 * implementation of a security-relevant header format.
 * <p>
 * Factor identifiers are echoed verbatim into a quoted-string parameter. Nothing is escaped here; safety comes from
 * {@link FactorRequirement} refusing to hold an identifier outside
 * {@link StepUpConstants#FACTOR_ID_PATTERN} in the first place, and from the policy loader rejecting such an
 * identifier when the configuration is read.
 */
public class StepUpChallengeBuilder {

    private StepUpChallengeBuilder() {

    }

    /**
     * Builds the {@code WWW-Authenticate} header value challenging for the unsatisfied factors of a decision result.
     *
     * @param requirements The decision result.
     * @return The header value, or empty if nothing in the result is unsatisfied and so there is nothing to
     * challenge for.
     */
    public static Optional<String> buildChallenge(List<FactorRequirement> requirements) {

        return buildChallenge(requirements, StepUpConstants.Challenge.DEFAULT_ERROR_DESCRIPTION);
    }

    /**
     * Builds the {@code WWW-Authenticate} header value challenging for the unsatisfied factors of a decision result.
     *
     * @param requirements     The decision result.
     * @param errorDescription Description to carry in the challenge.
     * @return The header value, or empty if nothing in the result is unsatisfied.
     * @throws IllegalArgumentException If the description carries a character that cannot appear unescaped in a
     *                                  quoted string.
     */
    public static Optional<String> buildChallenge(List<FactorRequirement> requirements, String errorDescription) {

        if (requirements == null || requirements.isEmpty()) {
            return Optional.empty();
        }
        List<FactorRequirement> unsatisfied = requirements.stream()
                .filter(requirement -> !requirement.isSatisfied())
                .collect(Collectors.toList());
        if (unsatisfied.isEmpty()) {
            return Optional.empty();
        }
        String description = errorDescription == null
                ? StepUpConstants.Challenge.DEFAULT_ERROR_DESCRIPTION : errorDescription;
        validateQuotedStringSafe(description);

        String acrValues = unsatisfied.stream()
                .map(FactorRequirement::getFactorId)
                .distinct()
                .collect(Collectors.joining(StepUpConstants.Challenge.ACR_VALUES_SEPARATOR));

        // The strictest window of the unsatisfied factors is what the caller must satisfy to get past the challenge.
        long maxAge = unsatisfied.stream()
                .mapToLong(FactorRequirement::getMaxAge)
                .min()
                .orElse(StepUpConstants.MAX_AGE_ALWAYS_PROMPT);

        StringBuilder challenge = new StringBuilder(StepUpConstants.Challenge.SCHEME)
                .append(' ')
                .append(StepUpConstants.Challenge.PARAM_ERROR).append("=\"")
                .append(StepUpConstants.Challenge.ERROR_CODE).append("\", ")
                .append(StepUpConstants.Challenge.PARAM_ERROR_DESCRIPTION).append("=\"")
                .append(description).append("\", ")
                .append(StepUpConstants.Challenge.PARAM_ACR_VALUES).append("=\"")
                .append(acrValues).append("\", ")
                .append(StepUpConstants.Challenge.PARAM_MAX_AGE).append('=')
                .append(maxAge);
        return Optional.of(challenge.toString());
    }

    private static void validateQuotedStringSafe(String value) {

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '"' || character == '\\' || character < 0x20 || character == 0x7F) {
                throw new IllegalArgumentException(
                        "Error description carries a character that cannot appear unescaped in a quoted string.");
            }
        }
    }
}
