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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
public class AuthenticationSequence {

    /**
     *
     */
    @XmlType(name = "TypeEnum")
    @XmlEnum(String.class)
    public enum TypeEnum {

        @XmlEnumValue("DEFAULT") DEFAULT(String.valueOf("DEFAULT")), @XmlEnumValue("USER_DEFINED") USER_DEFINED(
                String.valueOf("USER_DEFINED"));

        private String value;

        TypeEnum(String v) {

            value = v;
        }

        public String value() {

            return value;
        }

        @Override
        public String toString() {

            return String.valueOf(value);
        }

        public static TypeEnum fromValue(String value) {

            for (TypeEnum b : TypeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private TypeEnum type = TypeEnum.DEFAULT;
    private List<AuthenticationStepModel> steps = null;

    private List<String> requestPathAuthenticators = null;

    private String script;
    private Integer subjectStepId = 1;
    private Integer attributeStepId = 1;

    /**
     * - DEFAULT type indicates that the application will use the default authentication sequence specified at
     * the tenant level. When the DEFAULT type is used, the information given in the other fields of the
     * AuthenticationSequence will be ignored and overriden with values defined at the tenant level. -
     * USER_DEFINED type indicates that the application will use a user-defined authentication sequence.
     **/
    public AuthenticationSequence type(TypeEnum type) {

        this.type = type;
        return this;
    }

    @JsonProperty("type")
    public TypeEnum getType() {

        return type;
    }

    public void setType(TypeEnum type) {

        this.type = type;
    }

    /**
     *
     **/
    public AuthenticationSequence steps(List<AuthenticationStepModel> steps) {

        this.steps = steps;
        return this;
    }

    @JsonProperty("steps")
    public List<AuthenticationStepModel> getSteps() {

        return steps;
    }

    public void setSteps(List<AuthenticationStepModel> steps) {

        this.steps = steps;
    }

    public AuthenticationSequence addStepsItem(AuthenticationStepModel stepsItem) {

        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(stepsItem);
        return this;
    }

    /**
     *
     **/
    public AuthenticationSequence requestPathAuthenticators(List<String> requestPathAuthenticators) {

        this.requestPathAuthenticators = requestPathAuthenticators;
        return this;
    }

    @JsonProperty("requestPathAuthenticators")
    public List<String> getRequestPathAuthenticators() {

        return requestPathAuthenticators;
    }

    public void setRequestPathAuthenticators(List<String> requestPathAuthenticators) {

        this.requestPathAuthenticators = requestPathAuthenticators;
    }

    public AuthenticationSequence addRequestPathAuthenticatorsItem(String requestPathAuthenticatorsItem) {

        if (this.requestPathAuthenticators == null) {
            this.requestPathAuthenticators = new ArrayList<>();
        }
        this.requestPathAuthenticators.add(requestPathAuthenticatorsItem);
        return this;
    }

    /**
     *
     **/
    public AuthenticationSequence script(String script) {

        this.script = script;
        return this;
    }

    @JsonProperty("script")
    public String getScript() {

        return script;
    }

    public void setScript(String script) {

        this.script = script;
    }

    /**
     *
     **/
    public AuthenticationSequence subjectStepId(Integer subjectStepId) {

        this.subjectStepId = subjectStepId;
        return this;
    }

    @JsonProperty("subjectStepId")
    public Integer getSubjectStepId() {

        return subjectStepId;
    }

    public void setSubjectStepId(Integer subjectStepId) {

        this.subjectStepId = subjectStepId;
    }

    /**
     *
     **/
    public AuthenticationSequence attributeStepId(Integer attributeStepId) {

        this.attributeStepId = attributeStepId;
        return this;
    }

    @JsonProperty("attributeStepId")
    public Integer getAttributeStepId() {

        return attributeStepId;
    }

    public void setAttributeStepId(Integer attributeStepId) {

        this.attributeStepId = attributeStepId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticationSequence authenticationSequence = (AuthenticationSequence) o;
        return Objects.equals(this.type, authenticationSequence.type) &&
                Objects.equals(this.steps, authenticationSequence.steps) &&
                Objects.equals(this.requestPathAuthenticators, authenticationSequence.requestPathAuthenticators) &&
                Objects.equals(this.script, authenticationSequence.script) &&
                Objects.equals(this.subjectStepId, authenticationSequence.subjectStepId) &&
                Objects.equals(this.attributeStepId, authenticationSequence.attributeStepId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, steps, requestPathAuthenticators, script, subjectStepId, attributeStepId);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class AuthenticationSequence {\n");

        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    steps: ").append(toIndentedString(steps)).append("\n");
        sb.append("    requestPathAuthenticators: ").append(toIndentedString(requestPathAuthenticators)).append("\n");
        sb.append("    script: ").append(toIndentedString(script)).append("\n");
        sb.append("    subjectStepId: ").append(toIndentedString(subjectStepId)).append("\n");
        sb.append("    attributeStepId: ").append(toIndentedString(attributeStepId)).append("\n");
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

