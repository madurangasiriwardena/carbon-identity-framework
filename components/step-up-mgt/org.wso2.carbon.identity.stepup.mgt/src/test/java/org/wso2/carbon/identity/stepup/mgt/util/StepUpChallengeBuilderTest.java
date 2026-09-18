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

package org.wso2.carbon.identity.stepup.mgt.util;

import org.testng.annotations.Test;
import org.wso2.carbon.identity.stepup.mgt.api.model.FactorRequirement;
import org.wso2.carbon.identity.stepup.mgt.api.util.StepUpChallengeBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests of {@link StepUpChallengeBuilder}.
 */
public class StepUpChallengeBuilderTest {

    private static final String EMAIL_OTP = "email-otp-authenticator";
    private static final String SMS_OTP = "sms-otp-authenticator";

    @Test(description = "A single unsatisfied factor produces the RFC 9470 challenge.")
    public void testChallengeForSingleUnsatisfiedFactor() {

        Optional<String> challenge = StepUpChallengeBuilder.buildChallenge(
                Collections.singletonList(new FactorRequirement(EMAIL_OTP, 60, false)));

        assertTrue(challenge.isPresent());
        assertEquals(challenge.get(), "Bearer error=\"insufficient_user_authentication\", "
                + "error_description=\"A more recent authentication is required\", "
                + "acr_values=\"" + EMAIL_OTP + "\", max_age=60");
    }

    @Test(description = "Several unsatisfied factors are listed space separated, under the strictest window.")
    public void testChallengeForSeveralUnsatisfiedFactors() {

        Optional<String> challenge = StepUpChallengeBuilder.buildChallenge(Arrays.asList(
                new FactorRequirement(EMAIL_OTP, 600, false),
                new FactorRequirement(SMS_OTP, 60, false)));

        assertTrue(challenge.isPresent());
        assertTrue(challenge.get().contains("acr_values=\"" + EMAIL_OTP + " " + SMS_OTP + "\""));
        assertTrue(challenge.get().endsWith("max_age=60"));
    }

    @Test(description = "Factors the context already satisfies are not challenged for.")
    public void testSatisfiedFactorsAreExcluded() {

        Optional<String> challenge = StepUpChallengeBuilder.buildChallenge(Arrays.asList(
                new FactorRequirement(EMAIL_OTP, 600, true),
                new FactorRequirement(SMS_OTP, 60, false)));

        assertTrue(challenge.isPresent());
        assertFalse(challenge.get().contains(EMAIL_OTP));
        assertTrue(challenge.get().contains(SMS_OTP));
    }

    @Test(description = "A decision that is entirely satisfied carries no challenge.")
    public void testFullySatisfiedDecisionProducesNoChallenge() {

        assertFalse(StepUpChallengeBuilder.buildChallenge(
                Collections.singletonList(new FactorRequirement(EMAIL_OTP, 600, true))).isPresent());
    }

    @Test(description = "An empty or absent decision carries no challenge.")
    public void testEmptyDecisionProducesNoChallenge() {

        assertFalse(StepUpChallengeBuilder.buildChallenge(Collections.emptyList()).isPresent());
        assertFalse(StepUpChallengeBuilder.buildChallenge(null).isPresent());
    }

    @Test(description = "A maxAge of zero is carried into the challenge as zero.")
    public void testAlwaysPromptIsCarriedAsZero() {

        Optional<String> challenge = StepUpChallengeBuilder.buildChallenge(
                Collections.singletonList(new FactorRequirement(EMAIL_OTP, 0, false)));

        assertTrue(challenge.get().endsWith("max_age=0"));
    }

    @Test(description = "A factor identifier that could break out of the quoted string cannot be constructed, so "
            + "the builder never has to escape one.",
            expectedExceptions = IllegalArgumentException.class)
    public void testFactorRequirementRejectsIdentifierWithQuote() {

        new FactorRequirement("otp\", max_age=0, x=\"", 0, false);
    }

    @Test(description = "A factor identifier carrying a header separator cannot be constructed.",
            expectedExceptions = IllegalArgumentException.class)
    public void testFactorRequirementRejectsIdentifierWithControlCharacter() {

        new FactorRequirement("otp\r\nX-Injected: yes", 0, false);
    }

    @Test(description = "A negative freshness window cannot be constructed.",
            expectedExceptions = IllegalArgumentException.class)
    public void testFactorRequirementRejectsNegativeMaxAge() {

        new FactorRequirement(EMAIL_OTP, -1, false);
    }

    @Test(description = "A description that could break out of the quoted string is refused rather than escaped.",
            expectedExceptions = IllegalArgumentException.class)
    public void testUnsafeErrorDescriptionIsRefused() {

        StepUpChallengeBuilder.buildChallenge(
                Collections.singletonList(new FactorRequirement(EMAIL_OTP, 0, false)),
                "broken\" injected=\"yes");
    }
}
