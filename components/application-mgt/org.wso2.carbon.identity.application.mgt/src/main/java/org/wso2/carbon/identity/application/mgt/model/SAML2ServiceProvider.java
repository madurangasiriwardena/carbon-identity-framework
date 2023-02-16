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
public class SAML2ServiceProvider {

    private String issuer;
    private String serviceProviderQualifier;
    private List<String> assertionConsumerUrls = new ArrayList<>();

    private String defaultAssertionConsumerUrl;
    private String idpEntityIdAlias;
    private SingleSignOnProfile singleSignOnProfile;
    private SAMLAttributeProfile attributeProfile;
    private SingleLogoutProfile singleLogoutProfile;
    private SAMLRequestValidation requestValidation;
    private SAMLResponseSigning responseSigning;
    private Boolean enableAssertionQueryProfile = false;

    /**
     *
     **/
    public SAML2ServiceProvider issuer(String issuer) {

        this.issuer = issuer;
        return this;
    }

    @JsonProperty("issuer")

    public String getIssuer() {

        return issuer;
    }

    public void setIssuer(String issuer) {

        this.issuer = issuer;
    }

    /**
     *
     **/
    public SAML2ServiceProvider serviceProviderQualifier(String serviceProviderQualifier) {

        this.serviceProviderQualifier = serviceProviderQualifier;
        return this;
    }

    @JsonProperty("serviceProviderQualifier")
    public String getServiceProviderQualifier() {

        return serviceProviderQualifier;
    }

    public void setServiceProviderQualifier(String serviceProviderQualifier) {

        this.serviceProviderQualifier = serviceProviderQualifier;
    }

    /**
     *
     **/
    public SAML2ServiceProvider assertionConsumerUrls(List<String> assertionConsumerUrls) {

        this.assertionConsumerUrls = assertionConsumerUrls;
        return this;
    }

    @JsonProperty("assertionConsumerUrls")
    public List<String> getAssertionConsumerUrls() {

        return assertionConsumerUrls;
    }

    public void setAssertionConsumerUrls(List<String> assertionConsumerUrls) {

        this.assertionConsumerUrls = assertionConsumerUrls;
    }

    public SAML2ServiceProvider addAssertionConsumerUrlsItem(String assertionConsumerUrlsItem) {

        this.assertionConsumerUrls.add(assertionConsumerUrlsItem);
        return this;
    }

    /**
     * If not provided, the first assertion consumer URL on the assertionConsumerUrls will be picked as the
     * default assertion consumer URL.
     **/
    public SAML2ServiceProvider defaultAssertionConsumerUrl(String defaultAssertionConsumerUrl) {

        this.defaultAssertionConsumerUrl = defaultAssertionConsumerUrl;
        return this;
    }

    @JsonProperty("defaultAssertionConsumerUrl")
    public String getDefaultAssertionConsumerUrl() {

        return defaultAssertionConsumerUrl;
    }

    public void setDefaultAssertionConsumerUrl(String defaultAssertionConsumerUrl) {

        this.defaultAssertionConsumerUrl = defaultAssertionConsumerUrl;
    }

    /**
     * Default value is the IdP Entity ID value specified in Resident IdP.
     **/
    public SAML2ServiceProvider idpEntityIdAlias(String idpEntityIdAlias) {

        this.idpEntityIdAlias = idpEntityIdAlias;
        return this;
    }

    @JsonProperty("idpEntityIdAlias")
    public String getIdpEntityIdAlias() {

        return idpEntityIdAlias;
    }

    public void setIdpEntityIdAlias(String idpEntityIdAlias) {

        this.idpEntityIdAlias = idpEntityIdAlias;
    }

    /**
     *
     **/
    public SAML2ServiceProvider singleSignOnProfile(SingleSignOnProfile singleSignOnProfile) {

        this.singleSignOnProfile = singleSignOnProfile;
        return this;
    }

    @JsonProperty("singleSignOnProfile")
    public SingleSignOnProfile getSingleSignOnProfile() {

        return singleSignOnProfile;
    }

    public void setSingleSignOnProfile(SingleSignOnProfile singleSignOnProfile) {

        this.singleSignOnProfile = singleSignOnProfile;
    }

    /**
     *
     **/
    public SAML2ServiceProvider attributeProfile(SAMLAttributeProfile attributeProfile) {

        this.attributeProfile = attributeProfile;
        return this;
    }

    @JsonProperty("attributeProfile")
    public SAMLAttributeProfile getAttributeProfile() {

        return attributeProfile;
    }

    public void setAttributeProfile(SAMLAttributeProfile attributeProfile) {

        this.attributeProfile = attributeProfile;
    }

    /**
     *
     **/
    public SAML2ServiceProvider singleLogoutProfile(SingleLogoutProfile singleLogoutProfile) {

        this.singleLogoutProfile = singleLogoutProfile;
        return this;
    }

    @JsonProperty("singleLogoutProfile")
    public SingleLogoutProfile getSingleLogoutProfile() {

        return singleLogoutProfile;
    }

    public void setSingleLogoutProfile(SingleLogoutProfile singleLogoutProfile) {

        this.singleLogoutProfile = singleLogoutProfile;
    }

    /**
     *
     **/
    public SAML2ServiceProvider requestValidation(SAMLRequestValidation requestValidation) {

        this.requestValidation = requestValidation;
        return this;
    }

    @JsonProperty("requestValidation")
    public SAMLRequestValidation getRequestValidation() {

        return requestValidation;
    }

    public void setRequestValidation(SAMLRequestValidation requestValidation) {

        this.requestValidation = requestValidation;
    }

    /**
     *
     **/
    public SAML2ServiceProvider responseSigning(SAMLResponseSigning responseSigning) {

        this.responseSigning = responseSigning;
        return this;
    }

    @JsonProperty("responseSigning")
    public SAMLResponseSigning getResponseSigning() {

        return responseSigning;
    }

    public void setResponseSigning(SAMLResponseSigning responseSigning) {

        this.responseSigning = responseSigning;
    }

    /**
     *
     **/
    public SAML2ServiceProvider enableAssertionQueryProfile(Boolean enableAssertionQueryProfile) {

        this.enableAssertionQueryProfile = enableAssertionQueryProfile;
        return this;
    }

    @JsonProperty("enableAssertionQueryProfile")
    public Boolean getEnableAssertionQueryProfile() {

        return enableAssertionQueryProfile;
    }

    public void setEnableAssertionQueryProfile(Boolean enableAssertionQueryProfile) {

        this.enableAssertionQueryProfile = enableAssertionQueryProfile;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SAML2ServiceProvider saML2ServiceProvider = (SAML2ServiceProvider) o;
        return Objects.equals(this.issuer, saML2ServiceProvider.issuer) &&
                Objects.equals(this.serviceProviderQualifier, saML2ServiceProvider.serviceProviderQualifier) &&
                Objects.equals(this.assertionConsumerUrls, saML2ServiceProvider.assertionConsumerUrls) &&
                Objects.equals(this.defaultAssertionConsumerUrl, saML2ServiceProvider.defaultAssertionConsumerUrl) &&
                Objects.equals(this.idpEntityIdAlias, saML2ServiceProvider.idpEntityIdAlias) &&
                Objects.equals(this.singleSignOnProfile, saML2ServiceProvider.singleSignOnProfile) &&
                Objects.equals(this.attributeProfile, saML2ServiceProvider.attributeProfile) &&
                Objects.equals(this.singleLogoutProfile, saML2ServiceProvider.singleLogoutProfile) &&
                Objects.equals(this.requestValidation, saML2ServiceProvider.requestValidation) &&
                Objects.equals(this.responseSigning, saML2ServiceProvider.responseSigning) &&
                Objects.equals(this.enableAssertionQueryProfile, saML2ServiceProvider.enableAssertionQueryProfile);
    }

    @Override
    public int hashCode() {

        return Objects.hash(issuer, serviceProviderQualifier, assertionConsumerUrls, defaultAssertionConsumerUrl,
                idpEntityIdAlias, singleSignOnProfile, attributeProfile, singleLogoutProfile, requestValidation,
                responseSigning, enableAssertionQueryProfile);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class SAML2ServiceProvider {\n");

        sb.append("    issuer: ").append(toIndentedString(issuer)).append("\n");
        sb.append("    serviceProviderQualifier: ").append(toIndentedString(serviceProviderQualifier)).append("\n");
        sb.append("    assertionConsumerUrls: ").append(toIndentedString(assertionConsumerUrls)).append("\n");
        sb.append("    defaultAssertionConsumerUrl: ").append(toIndentedString(defaultAssertionConsumerUrl))
                .append("\n");
        sb.append("    idpEntityIdAlias: ").append(toIndentedString(idpEntityIdAlias)).append("\n");
        sb.append("    singleSignOnProfile: ").append(toIndentedString(singleSignOnProfile)).append("\n");
        sb.append("    attributeProfile: ").append(toIndentedString(attributeProfile)).append("\n");
        sb.append("    singleLogoutProfile: ").append(toIndentedString(singleLogoutProfile)).append("\n");
        sb.append("    requestValidation: ").append(toIndentedString(requestValidation)).append("\n");
        sb.append("    responseSigning: ").append(toIndentedString(responseSigning)).append("\n");
        sb.append("    enableAssertionQueryProfile: ").append(toIndentedString(enableAssertionQueryProfile))
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

