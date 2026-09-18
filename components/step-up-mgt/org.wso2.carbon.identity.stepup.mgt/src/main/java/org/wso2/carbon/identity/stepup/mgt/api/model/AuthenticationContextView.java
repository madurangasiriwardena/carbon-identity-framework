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

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A narrow, read-only view of an authentication context.
 * <p>
 * The decision component never accepts a token. A caller holding a token is responsible for parsing it into this
 * view; a caller holding a live authentication context adapts that context into this view. This is what keeps the
 * component free of any dependency on OAuth or on the authentication framework.
 * <p>
 * Two independent kinds of evidence of prior authentication are carried:
 * <ul>
 *     <li>{@link #getAchievedAcr()} - the ACR selected for the authentication. In this product an ACR is produced
 *     only when an adaptive authentication script assigns one, so it is absent in most deployments.</li>
 *     <li>{@link #getAchievedFactors()} - the authenticators or authentication methods the user actually completed.
 *     This is recorded by the framework on every authentication and is the evidence that is reliably present.</li>
 * </ul>
 * A factor requirement is satisfied by either, so a policy may name an ACR or a concrete authenticator.
 */
public final class AuthenticationContextView {

    private final String subjectId;
    private final String achievedAcr;
    private final Set<String> achievedFactors;
    private final Instant authTime;

    private AuthenticationContextView(Builder builder) {

        this.subjectId = builder.subjectId;
        this.achievedAcr = builder.achievedAcr;
        this.achievedFactors = Collections.unmodifiableSet(new LinkedHashSet<>(builder.achievedFactors));
        this.authTime = builder.authTime;
    }

    /**
     * Identifier of the authenticated subject, if known.
     *
     * @return The subject identifier.
     */
    public Optional<String> getSubjectId() {

        return Optional.ofNullable(subjectId);
    }

    /**
     * ACR achieved by the authentication, if one was selected.
     *
     * @return The achieved ACR.
     */
    public Optional<String> getAchievedAcr() {

        return Optional.ofNullable(achievedAcr);
    }

    /**
     * Authenticators or authentication methods the subject completed.
     *
     * @return The achieved factors. Never null, possibly empty.
     */
    public Set<String> getAchievedFactors() {

        return achievedFactors;
    }

    /**
     * Time at which the end-user authentication occurred.
     * <p>
     * An empty value means no known authentication event, which fails every freshness check.
     *
     * @return The authentication time.
     */
    public Optional<Instant> getAuthTime() {

        return Optional.ofNullable(authTime);
    }

    /**
     * Whether this context carries evidence of the given factor, by ACR or by completed authenticator.
     * <p>
     * Matching is exact. Authenticator names and ACR values are case sensitive in this product, so no case
     * folding is applied.
     *
     * @param factorId Factor identifier to look for.
     * @return true if the factor is evidenced by this context.
     */
    public boolean hasFactor(String factorId) {

        if (factorId == null) {
            return false;
        }
        return factorId.equals(achievedAcr) || achievedFactors.contains(factorId);
    }

    /**
     * Returns a builder for this view.
     *
     * @return A new builder.
     */
    public static Builder builder() {

        return new Builder();
    }

    /**
     * Builder of {@link AuthenticationContextView}.
     */
    public static class Builder {

        private String subjectId;
        private String achievedAcr;
        private Set<String> achievedFactors = new LinkedHashSet<>();
        private Instant authTime;

        public Builder subjectId(String subjectId) {

            this.subjectId = subjectId;
            return this;
        }

        public Builder achievedAcr(String achievedAcr) {

            this.achievedAcr = achievedAcr;
            return this;
        }

        public Builder achievedFactors(Set<String> achievedFactors) {

            this.achievedFactors = achievedFactors == null ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(achievedFactors);
            return this;
        }

        public Builder addAchievedFactor(String achievedFactor) {

            if (achievedFactor != null) {
                this.achievedFactors.add(achievedFactor);
            }
            return this;
        }

        public Builder authTime(Instant authTime) {

            this.authTime = authTime;
            return this;
        }

        public AuthenticationContextView build() {

            return new AuthenticationContextView(this);
        }
    }

    @Override
    public String toString() {

        return "AuthenticationContextView{achievedAcr='" + achievedAcr + "', achievedFactors="
                + achievedFactors + ", authTime=" + authTime + '}';
    }
}
