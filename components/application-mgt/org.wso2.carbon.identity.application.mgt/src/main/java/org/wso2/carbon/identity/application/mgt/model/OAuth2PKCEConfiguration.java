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
public class OAuth2PKCEConfiguration {

    private Boolean mandatory;
    private Boolean supportPlainTransformAlgorithm;

    /**
     *
     **/
    public OAuth2PKCEConfiguration mandatory(Boolean mandatory) {

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

    /**
     *
     **/
    public OAuth2PKCEConfiguration supportPlainTransformAlgorithm(Boolean supportPlainTransformAlgorithm) {

        this.supportPlainTransformAlgorithm = supportPlainTransformAlgorithm;
        return this;
    }

    @JsonProperty("supportPlainTransformAlgorithm")
    public Boolean getSupportPlainTransformAlgorithm() {

        return supportPlainTransformAlgorithm;
    }

    public void setSupportPlainTransformAlgorithm(Boolean supportPlainTransformAlgorithm) {

        this.supportPlainTransformAlgorithm = supportPlainTransformAlgorithm;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OAuth2PKCEConfiguration oauth2PKCEConfiguration = (OAuth2PKCEConfiguration) o;
        return Objects.equals(this.mandatory, oauth2PKCEConfiguration.mandatory) &&
                Objects.equals(this.supportPlainTransformAlgorithm,
                        oauth2PKCEConfiguration.supportPlainTransformAlgorithm);
    }

    @Override
    public int hashCode() {

        return Objects.hash(mandatory, supportPlainTransformAlgorithm);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class OAuth2PKCEConfiguration {\n");

        sb.append("    mandatory: ").append(toIndentedString(mandatory)).append("\n");
        sb.append("    supportPlainTransformAlgorithm: ").append(toIndentedString(supportPlainTransformAlgorithm))
                .append("\n");
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

