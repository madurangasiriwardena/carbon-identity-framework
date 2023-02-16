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
public class SAMLAssertionConfiguration {

    private String nameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
    private List<String> audiences = null;

    private List<String> recipients = null;

    private String digestAlgorithm = "http://www.w3.org/2000/09/xmldsig#sha1";
    private AssertionEncryptionConfiguration encryption;

    /**
     *
     **/
    public SAMLAssertionConfiguration nameIdFormat(String nameIdFormat) {

        this.nameIdFormat = nameIdFormat;
        return this;
    }

    @JsonProperty("nameIdFormat")
    public String getNameIdFormat() {

        return nameIdFormat;
    }

    public void setNameIdFormat(String nameIdFormat) {

        this.nameIdFormat = nameIdFormat;
    }

    /**
     * Additional audience values to be added to the SAML Assertions
     **/
    public SAMLAssertionConfiguration audiences(List<String> audiences) {

        this.audiences = audiences;
        return this;
    }

    @JsonProperty("audiences")
    public List<String> getAudiences() {

        return audiences;
    }

    public void setAudiences(List<String> audiences) {

        this.audiences = audiences;
    }

    public SAMLAssertionConfiguration addAudiencesItem(String audiencesItem) {

        if (this.audiences == null) {
            this.audiences = new ArrayList<>();
        }
        this.audiences.add(audiencesItem);
        return this;
    }

    /**
     * Additional recipient values to be added to the SAML Assertions
     **/
    public SAMLAssertionConfiguration recipients(List<String> recipients) {

        this.recipients = recipients;
        return this;
    }

    @JsonProperty("recipients")
    public List<String> getRecipients() {

        return recipients;
    }

    public void setRecipients(List<String> recipients) {

        this.recipients = recipients;
    }

    public SAMLAssertionConfiguration addRecipientsItem(String recipientsItem) {

        if (this.recipients == null) {
            this.recipients = new ArrayList<>();
        }
        this.recipients.add(recipientsItem);
        return this;
    }

    /**
     *
     **/
    public SAMLAssertionConfiguration digestAlgorithm(String digestAlgorithm) {

        this.digestAlgorithm = digestAlgorithm;
        return this;
    }

    @JsonProperty("digestAlgorithm")
    public String getDigestAlgorithm() {

        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {

        this.digestAlgorithm = digestAlgorithm;
    }

    /**
     *
     **/
    public SAMLAssertionConfiguration encryption(AssertionEncryptionConfiguration encryption) {

        this.encryption = encryption;
        return this;
    }

    @JsonProperty("encryption")
    public AssertionEncryptionConfiguration getEncryption() {

        return encryption;
    }

    public void setEncryption(AssertionEncryptionConfiguration encryption) {

        this.encryption = encryption;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SAMLAssertionConfiguration saMLAssertionConfiguration = (SAMLAssertionConfiguration) o;
        return Objects.equals(this.nameIdFormat, saMLAssertionConfiguration.nameIdFormat) &&
                Objects.equals(this.audiences, saMLAssertionConfiguration.audiences) &&
                Objects.equals(this.recipients, saMLAssertionConfiguration.recipients) &&
                Objects.equals(this.digestAlgorithm, saMLAssertionConfiguration.digestAlgorithm) &&
                Objects.equals(this.encryption, saMLAssertionConfiguration.encryption);
    }

    @Override
    public int hashCode() {

        return Objects.hash(nameIdFormat, audiences, recipients, digestAlgorithm, encryption);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class SAMLAssertionConfiguration {\n");

        sb.append("    nameIdFormat: ").append(toIndentedString(nameIdFormat)).append("\n");
        sb.append("    audiences: ").append(toIndentedString(audiences)).append("\n");
        sb.append("    recipients: ").append(toIndentedString(recipients)).append("\n");
        sb.append("    digestAlgorithm: ").append(toIndentedString(digestAlgorithm)).append("\n");
        sb.append("    encryption: ").append(toIndentedString(encryption)).append("\n");
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

