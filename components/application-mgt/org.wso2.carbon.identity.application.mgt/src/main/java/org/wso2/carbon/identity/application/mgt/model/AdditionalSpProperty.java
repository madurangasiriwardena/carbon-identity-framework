/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.application.mgt.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 *
 */
public class AdditionalSpProperty {

    private String name;
    private String value;
    private String displayName;

    /**
     *
     **/
    public AdditionalSpProperty name(String name) {

        this.name = name;
        return this;
    }

    @JsonProperty("name")

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    /**
     *
     **/
    public AdditionalSpProperty value(String value) {

        this.value = value;
        return this;
    }

    @JsonProperty("value")

    public String getValue() {

        return value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    /**
     *
     **/
    public AdditionalSpProperty displayName(String displayName) {

        this.displayName = displayName;
        return this;
    }

    @JsonProperty("displayName")
    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AdditionalSpProperty additionalSpProperty = (AdditionalSpProperty) o;
        return Objects.equals(this.name, additionalSpProperty.name) &&
                Objects.equals(this.value, additionalSpProperty.value) &&
                Objects.equals(this.displayName, additionalSpProperty.displayName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, value, displayName);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class AdditionalSpProperty {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
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

