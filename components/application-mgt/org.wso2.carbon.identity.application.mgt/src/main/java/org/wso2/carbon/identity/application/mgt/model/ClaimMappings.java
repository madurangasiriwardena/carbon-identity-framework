/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.application.mgt.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 *
 */
public class ClaimMappings {

    private String applicationClaim;
    private Claim localClaim;

    /**
     * Claim URI recieved by the application
     **/
    public ClaimMappings applicationClaim(String applicationClaim) {

        this.applicationClaim = applicationClaim;
        return this;
    }

    @JsonProperty("applicationClaim")

    public String getApplicationClaim() {

        return applicationClaim;
    }

    public void setApplicationClaim(String applicationClaim) {

        this.applicationClaim = applicationClaim;
    }

    /**
     *
     **/
    public ClaimMappings localClaim(Claim localClaim) {

        this.localClaim = localClaim;
        return this;
    }

    @JsonProperty("localClaim")

    public Claim getLocalClaim() {

        return localClaim;
    }

    public void setLocalClaim(Claim localClaim) {

        this.localClaim = localClaim;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClaimMappings claimMappings = (ClaimMappings) o;
        return Objects.equals(this.applicationClaim, claimMappings.applicationClaim) &&
                Objects.equals(this.localClaim, claimMappings.localClaim);
    }

    @Override
    public int hashCode() {

        return Objects.hash(applicationClaim, localClaim);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class ClaimMappings {\n");

        sb.append("    applicationClaim: ").append(toIndentedString(applicationClaim)).append("\n");
        sb.append("    localClaim: ").append(toIndentedString(localClaim)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {

        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n");
    }
}

