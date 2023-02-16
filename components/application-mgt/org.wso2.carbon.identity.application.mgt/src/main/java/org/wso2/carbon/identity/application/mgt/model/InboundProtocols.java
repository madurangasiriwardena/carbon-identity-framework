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
public class InboundProtocols {

    private SAML2Configuration saml;
    private OpenIDConnectConfiguration oidc;

    /**
     *
     **/
    public InboundProtocols saml(SAML2Configuration saml) {

        this.saml = saml;
        return this;
    }

    @JsonProperty("saml")
    public SAML2Configuration getSaml() {

        return saml;
    }

    public void setSaml(SAML2Configuration saml) {

        this.saml = saml;
    }

    /**
     *
     **/
    public InboundProtocols oidc(OpenIDConnectConfiguration oidc) {

        this.oidc = oidc;
        return this;
    }

    @JsonProperty("oidc")
    public OpenIDConnectConfiguration getOidc() {

        return oidc;
    }

    public void setOidc(OpenIDConnectConfiguration oidc) {

        this.oidc = oidc;
    }

    /**
     *
     **/

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InboundProtocols inboundProtocols = (InboundProtocols) o;
        return Objects.equals(this.saml, inboundProtocols.saml) &&
                Objects.equals(this.oidc, inboundProtocols.oidc);
    }

    @Override
    public int hashCode() {

        return Objects.hash(saml, oidc);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class InboundProtocols {\n");

        sb.append("    saml: ").append(toIndentedString(saml)).append("\n");
        sb.append("    oidc: ").append(toIndentedString(oidc)).append("\n");
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

