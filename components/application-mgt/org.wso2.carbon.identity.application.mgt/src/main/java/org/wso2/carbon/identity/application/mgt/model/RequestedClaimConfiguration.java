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
public class RequestedClaimConfiguration {

    private Claim claim;
    private Boolean mandatory;

    /**
     *
     **/
    public RequestedClaimConfiguration claim(Claim claim) {

        this.claim = claim;
        return this;
    }

    @JsonProperty("claim")

    public Claim getClaim() {

        return claim;
    }

    public void setClaim(Claim claim) {

        this.claim = claim;
    }

    /**
     *
     **/
    public RequestedClaimConfiguration mandatory(Boolean mandatory) {

        this.mandatory = mandatory;
        return this;
    }

    @JsonProperty("mandatory")
    public Boolean getMandatory() {

        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {

        this.mandatory = mandatory;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestedClaimConfiguration requestedClaimConfiguration = (RequestedClaimConfiguration) o;
        return Objects.equals(this.claim, requestedClaimConfiguration.claim) &&
                Objects.equals(this.mandatory, requestedClaimConfiguration.mandatory);
    }

    @Override
    public int hashCode() {

        return Objects.hash(claim, mandatory);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RequestedClaimConfiguration {\n");

        sb.append("    claim: ").append(toIndentedString(claim)).append("\n");
        sb.append("    mandatory: ").append(toIndentedString(mandatory)).append("\n");
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

