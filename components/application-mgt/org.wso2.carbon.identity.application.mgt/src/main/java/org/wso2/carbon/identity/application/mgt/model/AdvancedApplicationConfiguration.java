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
public class AdvancedApplicationConfiguration {

    private Boolean saas;
    private Boolean discoverableByEndUsers;
    private Certificate certificate;
    private Boolean skipLoginConsent;
    private Boolean skipLogoutConsent;
    private Boolean returnAuthenticatedIdpList;
    private Boolean enableAuthorization;
    private Boolean fragment;
    private List<AdditionalSpProperty> additionalSpProperties = null;

    /**
     * Decides whether the application is accessible across tenants.
     **/
    public AdvancedApplicationConfiguration saas(Boolean saas) {

        this.saas = saas;
        return this;
    }

    @JsonProperty("saas")
    public Boolean getSaas() {

        return saas;
    }

    public void setSaas(Boolean saas) {

        this.saas = saas;
    }

    /**
     * Decides whether the application is visible for end users.
     **/
    public AdvancedApplicationConfiguration discoverableByEndUsers(Boolean discoverableByEndUsers) {

        this.discoverableByEndUsers = discoverableByEndUsers;
        return this;
    }

    @JsonProperty("discoverableByEndUsers")
    public Boolean getDiscoverableByEndUsers() {

        return discoverableByEndUsers;
    }

    public void setDiscoverableByEndUsers(Boolean discoverableByEndUsers) {

        this.discoverableByEndUsers = discoverableByEndUsers;
    }

    /**
     *
     **/
    public AdvancedApplicationConfiguration certificate(Certificate certificate) {

        this.certificate = certificate;
        return this;
    }

    @JsonProperty("certificate")
    public Certificate getCertificate() {

        return certificate;
    }

    public void setCertificate(Certificate certificate) {

        this.certificate = certificate;
    }

    /**
     * Decides whether user consent needs to be skipped during login flows.
     **/
    public AdvancedApplicationConfiguration skipLoginConsent(Boolean skipLoginConsent) {

        this.skipLoginConsent = skipLoginConsent;
        return this;
    }

    @JsonProperty("skipLoginConsent")
    public Boolean getSkipLoginConsent() {

        return skipLoginConsent;
    }

    public void setSkipLoginConsent(Boolean skipLoginConsent) {

        this.skipLoginConsent = skipLoginConsent;
    }

    /**
     * Decides whether user consent needs to be skipped during logout flows.
     **/
    public AdvancedApplicationConfiguration skipLogoutConsent(Boolean skipLogoutConsent) {

        this.skipLogoutConsent = skipLogoutConsent;
        return this;
    }

    @JsonProperty("skipLogoutConsent")
    public Boolean getSkipLogoutConsent() {

        return skipLogoutConsent;
    }

    public void setSkipLogoutConsent(Boolean skipLogoutConsent) {

        this.skipLogoutConsent = skipLogoutConsent;
    }

    /**
     * Decides whether the list of authenticated identity providers need to be returned in the authentication response.
     **/
    public AdvancedApplicationConfiguration returnAuthenticatedIdpList(Boolean returnAuthenticatedIdpList) {

        this.returnAuthenticatedIdpList = returnAuthenticatedIdpList;
        return this;
    }

    @JsonProperty("returnAuthenticatedIdpList")
    public Boolean getReturnAuthenticatedIdpList() {

        return returnAuthenticatedIdpList;
    }

    public void setReturnAuthenticatedIdpList(Boolean returnAuthenticatedIdpList) {

        this.returnAuthenticatedIdpList = returnAuthenticatedIdpList;
    }

    /**
     * Decides whether authorization policies needs to be engaged during authentication flows.
     **/
    public AdvancedApplicationConfiguration enableAuthorization(Boolean enableAuthorization) {

        this.enableAuthorization = enableAuthorization;
        return this;
    }

    @JsonProperty("enableAuthorization")
    public Boolean getEnableAuthorization() {

        return enableAuthorization;
    }

    public void setEnableAuthorization(Boolean enableAuthorization) {

        this.enableAuthorization = enableAuthorization;
    }

    /**
     * Decides whether application is a fragment application.
     **/
    public AdvancedApplicationConfiguration fragment(Boolean fragment) {

        this.fragment = fragment;
        return this;
    }

    @JsonProperty("fragment")
    public Boolean getFragment() {

        return fragment;
    }

    public void setFragment(Boolean fragment) {

        this.fragment = fragment;
    }

    /**
     *
     **/
    public AdvancedApplicationConfiguration additionalSpProperties(List<AdditionalSpProperty> additionalSpProperties) {

        this.additionalSpProperties = additionalSpProperties;
        return this;
    }

    @JsonProperty("additionalSpProperties")
    public List<AdditionalSpProperty> getAdditionalSpProperties() {

        return additionalSpProperties;
    }

    public void setAdditionalSpProperties(List<AdditionalSpProperty> additionalSpProperties) {

        this.additionalSpProperties = additionalSpProperties;
    }

    public AdvancedApplicationConfiguration addAdditionalSpPropertiesItem(
            AdditionalSpProperty additionalSpPropertiesItem) {

        if (this.additionalSpProperties == null) {
            this.additionalSpProperties = new ArrayList<>();
        }
        this.additionalSpProperties.add(additionalSpPropertiesItem);
        return this;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AdvancedApplicationConfiguration advancedApplicationConfiguration = (AdvancedApplicationConfiguration) o;
        return Objects.equals(this.saas, advancedApplicationConfiguration.saas) &&
                Objects.equals(this.discoverableByEndUsers, advancedApplicationConfiguration.discoverableByEndUsers) &&
                Objects.equals(this.certificate, advancedApplicationConfiguration.certificate) &&
                Objects.equals(this.skipLoginConsent, advancedApplicationConfiguration.skipLoginConsent) &&
                Objects.equals(this.skipLogoutConsent, advancedApplicationConfiguration.skipLogoutConsent) &&
                Objects.equals(this.returnAuthenticatedIdpList,
                        advancedApplicationConfiguration.returnAuthenticatedIdpList) &&
                Objects.equals(this.enableAuthorization, advancedApplicationConfiguration.enableAuthorization) &&
                Objects.equals(this.fragment, advancedApplicationConfiguration.fragment) &&
                Objects.equals(this.additionalSpProperties, advancedApplicationConfiguration.additionalSpProperties);
    }

    @Override
    public int hashCode() {

        return Objects.hash(saas, discoverableByEndUsers, certificate, skipLoginConsent, skipLogoutConsent,
                returnAuthenticatedIdpList, enableAuthorization, fragment, additionalSpProperties);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class AdvancedApplicationConfiguration {\n");

        sb.append("    saas: ").append(toIndentedString(saas)).append("\n");
        sb.append("    discoverableByEndUsers: ").append(toIndentedString(discoverableByEndUsers)).append("\n");
        sb.append("    certificate: ").append(toIndentedString(certificate)).append("\n");
        sb.append("    skipLoginConsent: ").append(toIndentedString(skipLoginConsent)).append("\n");
        sb.append("    skipLogoutConsent: ").append(toIndentedString(skipLogoutConsent)).append("\n");
        sb.append("    returnAuthenticatedIdpList: ").append(toIndentedString(returnAuthenticatedIdpList)).append("\n");
        sb.append("    enableAuthorization: ").append(toIndentedString(enableAuthorization)).append("\n");
        sb.append("    fragment: ").append(toIndentedString(fragment)).append("\n");
        sb.append("    additionalSpProperties: ").append(toIndentedString(additionalSpProperties)).append("\n");
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

