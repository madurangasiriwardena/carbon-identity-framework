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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class RoleConfig {

    private List<RoleMapping> mappings = null;

    private Boolean includeUserDomain;
    private Claim claim;

    /**
     *
     **/
    public RoleConfig mappings(List<RoleMapping> mappings) {

        this.mappings = mappings;
        return this;
    }

    @JsonProperty("mappings")
    public List<RoleMapping> getMappings() {

        return mappings;
    }

    public void setMappings(List<RoleMapping> mappings) {

        this.mappings = mappings;
    }

    public RoleConfig addMappingsItem(RoleMapping mappingsItem) {

        if (this.mappings == null) {
            this.mappings = new ArrayList<>();
        }
        this.mappings.add(mappingsItem);
        return this;
    }

    /**
     *
     **/
    public RoleConfig includeUserDomain(Boolean includeUserDomain) {

        this.includeUserDomain = includeUserDomain;
        return this;
    }

    @JsonProperty("includeUserDomain")
    public Boolean getIncludeUserDomain() {

        return includeUserDomain;
    }

    public void setIncludeUserDomain(Boolean includeUserDomain) {

        this.includeUserDomain = includeUserDomain;
    }

    /**
     *
     **/
    public RoleConfig claim(Claim claim) {

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

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleConfig roleConfig = (RoleConfig) o;
        return Objects.equals(this.mappings, roleConfig.mappings) &&
                Objects.equals(this.includeUserDomain, roleConfig.includeUserDomain) &&
                Objects.equals(this.claim, roleConfig.claim);
    }

    @Override
    public int hashCode() {

        return Objects.hash(mappings, includeUserDomain, claim);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RoleConfig {\n");

        sb.append("    mappings: ").append(toIndentedString(mappings)).append("\n");
        sb.append("    includeUserDomain: ").append(toIndentedString(includeUserDomain)).append("\n");
        sb.append("    claim: ").append(toIndentedString(claim)).append("\n");
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

