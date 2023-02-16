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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationModel {

    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String accessUrl;
    private String templateId;
    private Boolean isManagementApp = false;
    private ClaimConfiguration claimConfiguration;
    private InboundProtocols inboundProtocolConfiguration;
    private AuthenticationSequence authenticationSequence;
    private AdvancedApplicationConfiguration advancedConfigurations;
    private ProvisioningConfiguration provisioningConfigurations;

    /**
     *
     **/
    public ApplicationModel id(String id) {

        this.id = id;
        return this;
    }

    @JsonProperty("id")
    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    /**
     *
     **/
    public ApplicationModel name(String name) {

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
    public ApplicationModel description(String description) {

        this.description = description;
        return this;
    }

    @JsonProperty("description")
    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    /**
     *
     **/
    public ApplicationModel imageUrl(String imageUrl) {

        this.imageUrl = imageUrl;
        return this;
    }

    @JsonProperty("imageUrl")
    public String getImageUrl() {

        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {

        this.imageUrl = imageUrl;
    }

    /**
     *
     **/
    public ApplicationModel accessUrl(String accessUrl) {

        this.accessUrl = accessUrl;
        return this;
    }

    @JsonProperty("accessUrl")
    public String getAccessUrl() {

        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {

        this.accessUrl = accessUrl;
    }

    /**
     *
     **/
    public ApplicationModel templateId(String templateId) {

        this.templateId = templateId;
        return this;
    }

    @JsonProperty("templateId")
    public String getTemplateId() {

        return templateId;
    }

    public void setTemplateId(String templateId) {

        this.templateId = templateId;
    }

    /**
     * Decides whether the application used to access System APIs
     **/
    public ApplicationModel isManagementApp(Boolean isManagementApp) {

        this.isManagementApp = isManagementApp;
        return this;
    }

    @JsonProperty("isManagementApp")
    public Boolean getIsManagementApp() {

        return isManagementApp;
    }

    public void setIsManagementApp(Boolean isManagementApp) {

        this.isManagementApp = isManagementApp;
    }

    /**
     *
     **/
    public ApplicationModel claimConfiguration(ClaimConfiguration claimConfiguration) {

        this.claimConfiguration = claimConfiguration;
        return this;
    }

    @JsonProperty("claimConfiguration")
    public ClaimConfiguration getClaimConfiguration() {

        return claimConfiguration;
    }

    public void setClaimConfiguration(ClaimConfiguration claimConfiguration) {

        this.claimConfiguration = claimConfiguration;
    }

    /**
     *
     **/
    public ApplicationModel inboundProtocolConfiguration(InboundProtocols inboundProtocolConfiguration) {

        this.inboundProtocolConfiguration = inboundProtocolConfiguration;
        return this;
    }

    @JsonProperty("inboundProtocolConfiguration")
    public InboundProtocols getInboundProtocolConfiguration() {

        return inboundProtocolConfiguration;
    }

    public void setInboundProtocolConfiguration(InboundProtocols inboundProtocolConfiguration) {

        this.inboundProtocolConfiguration = inboundProtocolConfiguration;
    }

    /**
     *
     **/
    public ApplicationModel authenticationSequence(AuthenticationSequence authenticationSequence) {

        this.authenticationSequence = authenticationSequence;
        return this;
    }

    @JsonProperty("authenticationSequence")
    public AuthenticationSequence getAuthenticationSequence() {

        return authenticationSequence;
    }

    public void setAuthenticationSequence(AuthenticationSequence authenticationSequence) {

        this.authenticationSequence = authenticationSequence;
    }

    /**
     *
     **/
    public ApplicationModel advancedConfigurations(AdvancedApplicationConfiguration advancedConfigurations) {

        this.advancedConfigurations = advancedConfigurations;
        return this;
    }

    @JsonProperty("advancedConfigurations")
    public AdvancedApplicationConfiguration getAdvancedConfigurations() {

        return advancedConfigurations;
    }

    public void setAdvancedConfigurations(AdvancedApplicationConfiguration advancedConfigurations) {

        this.advancedConfigurations = advancedConfigurations;
    }

    /**
     *
     **/
    public ApplicationModel provisioningConfigurations(ProvisioningConfiguration provisioningConfigurations) {

        this.provisioningConfigurations = provisioningConfigurations;
        return this;
    }

    @JsonProperty("provisioningConfigurations")
    public ProvisioningConfiguration getProvisioningConfigurations() {

        return provisioningConfigurations;
    }

    public void setProvisioningConfigurations(ProvisioningConfiguration provisioningConfigurations) {

        this.provisioningConfigurations = provisioningConfigurations;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApplicationModel applicationModel = (ApplicationModel) o;
        return Objects.equals(this.id, applicationModel.id) &&
                Objects.equals(this.name, applicationModel.name) &&
                Objects.equals(this.description, applicationModel.description) &&
                Objects.equals(this.imageUrl, applicationModel.imageUrl) &&
                Objects.equals(this.accessUrl, applicationModel.accessUrl) &&
                Objects.equals(this.templateId, applicationModel.templateId) &&
                Objects.equals(this.isManagementApp, applicationModel.isManagementApp) &&
                Objects.equals(this.claimConfiguration, applicationModel.claimConfiguration) &&
                Objects.equals(this.inboundProtocolConfiguration, applicationModel.inboundProtocolConfiguration) &&
                Objects.equals(this.authenticationSequence, applicationModel.authenticationSequence) &&
                Objects.equals(this.advancedConfigurations, applicationModel.advancedConfigurations) &&
                Objects.equals(this.provisioningConfigurations, applicationModel.provisioningConfigurations);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name, description, imageUrl, accessUrl, templateId, isManagementApp, claimConfiguration,
                inboundProtocolConfiguration, authenticationSequence, advancedConfigurations,
                provisioningConfigurations);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class ApplicationModel {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    imageUrl: ").append(toIndentedString(imageUrl)).append("\n");
        sb.append("    accessUrl: ").append(toIndentedString(accessUrl)).append("\n");
        sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
        sb.append("    isManagementApp: ").append(toIndentedString(isManagementApp)).append("\n");
        sb.append("    claimConfiguration: ").append(toIndentedString(claimConfiguration)).append("\n");
        sb.append("    inboundProtocolConfiguration: ").append(toIndentedString(inboundProtocolConfiguration))
                .append("\n");
        sb.append("    authenticationSequence: ").append(toIndentedString(authenticationSequence)).append("\n");
        sb.append("    advancedConfigurations: ").append(toIndentedString(advancedConfigurations)).append("\n");
        sb.append("    provisioningConfigurations: ").append(toIndentedString(provisioningConfigurations)).append("\n");
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

