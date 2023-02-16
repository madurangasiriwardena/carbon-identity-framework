package org.wso2.carbon.identity.application.mgt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.ImportResponse;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.SpFileContent;
import org.wso2.carbon.identity.application.common.model.SpTemplate;
import org.wso2.carbon.identity.application.mgt.functions.ApiModelToServiceProvider;
import org.wso2.carbon.identity.application.mgt.functions.UpdateInboundProtocols;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationMgtListenerServiceComponent;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.application.mgt.model.ApplicationModel;
import org.wso2.carbon.identity.application.mgt.model.InboundProtocols;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Application management service implementation in DP.
 */
public class DPApplicationManagementServiceImpl extends ApplicationManagementService {

    private static final Log LOG = LogFactory.getLog(DPApplicationManagementServiceImpl.class);
    private static final DPApplicationManagementServiceImpl INSTANCE =
            new DPApplicationManagementServiceImpl();
    public static final String CP_BASE_URL = "https://localhost:9443";

    public static ApplicationManagementService getInstance() {

        return INSTANCE;
    }

    @Override
    public ServiceProvider addApplication(ServiceProvider serviceProvider, String tenantDomain, String username)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ServiceProvider createApplicationWithTemplate(ServiceProvider serviceProvider, String tenantDomain,
                                                         String username, String templateName)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ServiceProvider getApplicationExcludingFileBasedSPs(String applicationName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ApplicationBasicInfo[] getAllApplicationBasicInfo(String tenantDomain, String username)
            throws IdentityApplicationManagementException {

        return new ApplicationBasicInfo[0];
    }

    @Override
    public ApplicationBasicInfo[] getApplicationBasicInfo(String tenantDomain, String username, String filter)
            throws IdentityApplicationManagementException {

        return new ApplicationBasicInfo[0];
    }

    @Override
    public ApplicationBasicInfo[] getAllPaginatedApplicationBasicInfo(String tenantDomain, String username,
                                                                      int pageNumber)
            throws IdentityApplicationManagementException {

        return new ApplicationBasicInfo[0];
    }

    @Override
    public ApplicationBasicInfo[] getPaginatedApplicationBasicInfo(String tenantDomain, String username, int pageNumber,
                                                                   String filter)
            throws IdentityApplicationManagementException {

        return new ApplicationBasicInfo[0];
    }

    @Override
    public int getCountOfAllApplications(String tenantDomain, String username)
            throws IdentityApplicationManagementException {

        return 0;
    }

    @Override
    public int getCountOfApplications(String tenantDomain, String username, String filter)
            throws IdentityApplicationManagementException {

        return 0;
    }

    @Override
    public void updateApplication(ServiceProvider serviceProvider, String tenantDomain, String username)
            throws IdentityApplicationManagementException {

    }

    @Override
    public void deleteApplication(String applicationName, String tenantDomain, String username)
            throws IdentityApplicationManagementException {

    }

    @Override
    public void deleteApplications(int tenantId) throws IdentityApplicationManagementException {

    }

    @Override
    public IdentityProvider getIdentityProvider(String federatedIdPName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public IdentityProvider[] getAllIdentityProviders(String tenantDomain)
            throws IdentityApplicationManagementException {

        return new IdentityProvider[0];
    }

    @Override
    public LocalAuthenticatorConfig[] getAllLocalAuthenticators(String tenantDomain)
            throws IdentityApplicationManagementException {

        return new LocalAuthenticatorConfig[0];
    }

    @Override
    public RequestPathAuthenticatorConfig[] getAllRequestPathAuthenticators(String tenantDomain)
            throws IdentityApplicationManagementException {

        return new RequestPathAuthenticatorConfig[0];
    }

    @Override
    public String[] getAllLocalClaimUris(String tenantDomain) throws IdentityApplicationManagementException {

        return new String[0];
    }

    @Override
    public String getServiceProviderNameByClientIdExcludingFileBasedSPs(String clientId, String type,
                                                                        String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public Map<String, String> getServiceProviderToLocalIdPClaimMapping(String serviceProviderName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public Map<String, String> getLocalIdPToServiceProviderClaimMapping(String serviceProviderName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public List<String> getAllRequestedClaimsByServiceProvider(String serviceProviderName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public String getServiceProviderNameByClientId(String clientId, String type, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ServiceProvider getServiceProvider(String serviceProviderName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ServiceProvider getServiceProvider(int appId) throws IdentityApplicationManagementException {

        return null;
    }

    // todo need to implement
    @Override
    public ServiceProvider getServiceProviderByClientId(String clientId, String clientType, String tenantDomain)
            throws IdentityApplicationManagementException {

        // invoking the listeners
        Collection<ApplicationMgtListener> listeners = getApplicationMgtListeners();
        for (ApplicationMgtListener listener : listeners) {
            if (listener.isEnable() && !listener.doPreGetServiceProviderByClientId(clientId, clientType,
                    tenantDomain)) {
                return null;
            }
        }
        // client id can contain the @ to identify the tenant domain.
        if (clientId != null && clientId.contains("@")) {
            clientId = clientId.split("@")[0];
        }

        //TODO works with OIDC only
        String applicationId = getApplicationId(clientId, clientType, tenantDomain);
        ServiceProvider serviceProvider = buildServiceProvider(tenantDomain, applicationId);
        populateInboundProtocals(tenantDomain, applicationId, serviceProvider);

        //TODO do we need to separately get the file based SPs?

        for (ApplicationMgtListener listener : listeners) {
            if (listener.isEnable() && !listener.doPostGetServiceProviderByClientId(serviceProvider, clientId,
                    clientType, tenantDomain)) {
                return null;
            }
        }

        return serviceProvider;
    }

    private static String getApplicationId(String clientId, String clientType, String tenantDomain)
            throws IdentityApplicationManagementException {

        String url = null;
        if ("oauth2".equals(clientType)) {
            url = String.format(
                    CP_BASE_URL + "/t/%s/api/server/v1/applications?filter=clientId+eq+%s&limit=2&offset=0",
                    tenantDomain, clientId);
        }

        String filteredList = callControlPlane(url);
        JSONObject jsonResponse = new JSONObject(new JSONTokener(filteredList));
        JSONArray applications = jsonResponse.getJSONArray("applications");
        if (applications.length() != 1) {
            throw new IdentityApplicationManagementException("Could not find a unique application for client id: " +
                    clientId);
        }

        JSONObject filteredApplication = (JSONObject) applications.get(0);
        String applicationId = filteredApplication.getString("id");
        return applicationId;
    }

    private static ServiceProvider buildServiceProvider(String tenantDomain, String applicationId)
            throws IdentityApplicationManagementException {

        String url;
        url = String.format(CP_BASE_URL + "/t/%s/api/server/v1/applications/%s", tenantDomain, applicationId);
        String applicationString = callControlPlane(url);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationModel applicationModel;
        try {
            applicationModel = mapper.readValue(applicationString, ApplicationModel.class);
        } catch (JsonProcessingException e) {
            throw new IdentityApplicationManagementException("Error while parsing the application model", e);
        }

        ServiceProvider serviceProvider = new ApiModelToServiceProvider().apply(applicationModel);
        return serviceProvider;
    }

    private static void populateInboundProtocals(String tenantDomain, String applicationId,
                                                 ServiceProvider serviceProvider)
            throws IdentityApplicationManagementException {

        String url;
        ObjectMapper mapper;
        url = String.format(CP_BASE_URL + "/t/%s/api/server/v1/applications/%s/inbound-protocols/oidc",
                tenantDomain, applicationId);
        String oidcString = callControlPlane(url);
        mapper = new ObjectMapper();
        try {
            InboundProtocols inboundProtocols = mapper.readValue(oidcString, InboundProtocols.class);
            new UpdateInboundProtocols().apply(serviceProvider, inboundProtocols);
        } catch (JsonProcessingException e) {
            throw new IdentityApplicationManagementException("Error while parsing the application model", e);
        }
    }

    /**
     * Returns the Collection of ApplicationMgtListener, discovered via the component.
     *
     * @return Collection of ApplicationMgtListener
     */
    private Collection<ApplicationMgtListener> getApplicationMgtListeners() {

        return ApplicationMgtListenerServiceComponent.getApplicationMgtListeners();
    }

    private static String callControlPlane(String url) throws IdentityApplicationManagementException {

//        final HttpHost targetHost = new HttpHost("localhost", 9443, "https");
//        final BasicCredentialsProvider provider = new BasicCredentialsProvider();
//
//        provider.setCredentials(
//                new AuthScope(targetHost),
//                new UsernamePasswordCredentials("user", "password"));
//
//        AuthCache authCache = new BasicAuthCache();
//        authCache.put(targetHost, new BasicScheme());

//        HttpClientContext localContext = HttpClientContext.create();
//        localContext.setAuthCache(authCache);
        //        AuthScope authScope = new AuthScope(targetHost);
        //        provider.setCredentials(authScope, new UsernamePasswordCredentials("admin", "admin"));

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpGet request = new HttpGet(url);
            final byte[] encodedAuth = Base64.encodeBase64("admin:admin".getBytes(StandardCharsets.UTF_8));
            final String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
            request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

            try (CloseableHttpResponse response = httpClient.execute(request)) {

                // Get HttpResponse Status
                LOG.info(response.getProtocolVersion());              // HTTP/1.1
                LOG.info(response.getStatusLine().getStatusCode());   // 200
                LOG.info(response.getStatusLine().getReasonPhrase()); // OK
                LOG.info(response.getStatusLine().toString());        // HTTP/1.1 200 OK

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // return it as a String
                    String result = EntityUtils.toString(entity);
                    return result;
                }
            }
        } catch (IOException e) {
            throw new IdentityApplicationManagementException("Error while invoking the control plane.", e);
        }
        throw new IdentityApplicationManagementException("No data found.");
    }

    @Override
    public ServiceProvider getApplicationWithRequiredAttributes(int applicationId, List<String> requiredAttributes)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public String exportSPApplication(String applicationName, boolean exportSecrets, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ImportResponse importSPApplication(SpFileContent spFileContent, String tenantDomain, String username,
                                              boolean isUpdate) throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ImportResponse importSPApplication(ServiceProvider serviceProvider, String tenantDomain, String username,
                                              boolean isUpdate) throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public void createApplicationTemplate(SpTemplate spTemplate, String tenantDomain)
            throws IdentityApplicationManagementException {

    }

    @Override
    public void createApplicationTemplateFromSP(ServiceProvider serviceProvider, SpTemplate spTemplate,
                                                String tenantDomain) throws IdentityApplicationManagementException {

    }

    @Override
    public SpTemplate getApplicationTemplate(String templateName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public void deleteApplicationTemplate(String templateName, String tenantDomain)
            throws IdentityApplicationManagementException {

    }

    @Override
    public void updateApplicationTemplate(String templateName, SpTemplate spTemplate, String tenantDomain)
            throws IdentityApplicationManagementException {

    }

    @Override
    public boolean isExistingApplicationTemplate(String templateName, String tenantDomain)
            throws IdentityApplicationManagementException {

        return false;
    }

    @Override
    public List<SpTemplate> getAllApplicationTemplateInfo(String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public AuthenticationStep[] getConfiguredAuthenticators(String applicationID)
            throws IdentityApplicationManagementException {

        return new AuthenticationStep[0];
    }

    @Override
    public ApplicationBasicInfo getApplicationBasicInfoByResourceId(String resourceId, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public String createApplication(ServiceProvider application, String tenantDomain, String username)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public ServiceProvider getApplicationByResourceId(String resourceId, String tenantDomain)
            throws IdentityApplicationManagementException {

        return null;
    }

    @Override
    public void updateApplicationByResourceId(String resourceId, ServiceProvider updatedApplication,
                                              String tenantDomain, String username)
            throws IdentityApplicationManagementException {

    }

    @Override
    public void deleteApplicationByResourceId(String resourceId, String tenantDomain, String username)
            throws IdentityApplicationManagementException {

    }
}
