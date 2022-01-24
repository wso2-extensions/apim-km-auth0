/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.auth0.client;

import com.google.gson.Gson;
import feign.Feign;
import feign.FeignException;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.auth0.client.model.Auth0AccessTokenResponse;
import org.wso2.auth0.client.model.Auth0APIKeyInterceptor;
import org.wso2.auth0.client.model.Auth0ClientInfo;
import org.wso2.auth0.client.model.Auth0ClientGrant;
import org.wso2.auth0.client.model.Auth0ClientGrantInfo;
import org.wso2.auth0.client.model.Auth0DCRClient;
import org.wso2.auth0.client.model.Auth0TokenClient;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.AccessTokenRequest;
import org.wso2.carbon.apimgt.api.model.ApplicationConstants;
import org.wso2.carbon.apimgt.api.model.KeyManagerConfiguration;
import org.wso2.carbon.apimgt.api.model.OAuthAppRequest;
import org.wso2.carbon.apimgt.api.model.OAuthApplicationInfo;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.AbstractKeyManager;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.kmclient.FormEncoder;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Auth0 Client Implementation.
 */
public class Auth0OAuthClient extends AbstractKeyManager {
    private static final Log log = LogFactory.getLog(Auth0OAuthClient.class);
    private Auth0DCRClient auth0DCRClient;
    private Auth0ClientGrant auth0ClientGrant;
    private Auth0TokenClient auth0TokenClient;

    /**
     * Returns base64 encoded credentials.
     *
     * @param clientId     clientId of the oauth client.
     * @param clientSecret clientSecret of the oauth clients.
     * @return String base64 encode string.
     */
    public static String getEncodedCredentials(String clientId, String clientSecret) throws APIManagementException {

        String encodedCredentials;
        try {
            encodedCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret)
                    .getBytes(Auth0Constants.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new APIManagementException(Auth0Constants.ERROR_ENCODING_METHOD_NOT_SUPPORTED, e);
        }

        return encodedCredentials;
    }

    @Override
    public OAuthApplicationInfo createApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {
        OAuthApplicationInfo oAuthApplicationInfo = oAuthAppRequest.getOAuthApplicationInfo();
        Auth0ClientInfo clientInfo = createClientInfoFromOauthApplicationInfo(oAuthApplicationInfo);
        Auth0ClientInfo createdApplication = auth0DCRClient.createApplication(clientInfo);
        if (createdApplication != null) {
            OAuthApplicationInfo createdOauthApplication = createOAuthAppInfoFromResponse(createdApplication);
            String audience = getAudienceFromAuthAppRequest(oAuthApplicationInfo);
            Auth0ClientGrantInfo auth0ClientGrantInfo = new Auth0ClientGrantInfo(createdApplication.getClientId(),
                    audience);
            Auth0ClientGrantInfo addedClientGrant = null;
            if (!audience.isEmpty()) {
                addedClientGrant = auth0ClientGrant.createClientGrant(auth0ClientGrantInfo);
            } else {
                log.warn("Did not provide the audience");
                return createdOauthApplication;
            }
            if (addedClientGrant == null) {
                log.warn("Error while adding the audience");
            }
            return createdOauthApplication;
        }
        return null;
    }

    /**
     * This method will extract Audience from the {@code OAuthApplicationInfo} object.
     *
     * @param oAuthApplicationInfo oauth Application info object when creating an application.
     * @return Audience specified in the developer portal application configuration section.
     */
    private String getAudienceFromAuthAppRequest(OAuthApplicationInfo oAuthApplicationInfo) {
        Object parameter = oAuthApplicationInfo.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
        Map<String, Object> additionalProperties = new HashMap<>();
        if (parameter instanceof String) {
            additionalProperties = new Gson().fromJson((String) parameter, Map.class);
        }
        return (String) additionalProperties.get(Auth0Constants.API_AUDIENCE);
    }

    /**
     * This method will create {@code OAuthApplicationInfo} object from a Map of Attributes.
     *
     * @param createdApplication Response returned from server as a Map
     * @return OAuthApplicationInfo object will return.
     */
    private OAuthApplicationInfo createOAuthAppInfoFromResponse(Auth0ClientInfo createdApplication) {
        OAuthApplicationInfo appInfo = new OAuthApplicationInfo();
        appInfo.setClientName(createdApplication.getClientName());
        appInfo.setClientId(createdApplication.getClientId());
        appInfo.setClientSecret(createdApplication.getClientSecret());
        String audience = "";

        if (createdApplication.getRedirectUris() != null && createdApplication.getRedirectUris().size() > 0) {
            appInfo.setCallBackURL(String.join(",", createdApplication.getRedirectUris()));
        }
        if (StringUtils.isNotEmpty(createdApplication.getClientName())) {
            appInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_NAME, createdApplication.getClientName());
        }
        if (StringUtils.isNotEmpty(createdApplication.getClientId())) {
            appInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_ID, createdApplication.getClientId());
            Auth0ClientGrantInfo[] clientGrantInfos = auth0ClientGrant.getClientGrant(createdApplication.getClientId());
            audience = clientGrantInfos.length > 0 ? clientGrantInfos[0].getAudience() : "";
        }
        if (StringUtils.isNotEmpty(createdApplication.getClientSecret())) {
            appInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_SECRET, createdApplication.getClientSecret());
        }
        if (createdApplication.getGrantTypes() != null && createdApplication.getGrantTypes().size() > 0) {
            appInfo.addParameter(APIConstants.JSON_GRANT_TYPES, String.join(" ", createdApplication.getGrantTypes()));
        }

        String additionalProperties = new Gson().toJson(createdApplication);
        Map additionalPropMap = new Gson().fromJson(additionalProperties, Map.class);
        additionalPropMap.put(Auth0Constants.API_AUDIENCE, audience);
        appInfo.addParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES, additionalPropMap);
        return appInfo;
    }

    /**
     * This method can be used to create a JSON Payload out of the Parameters defined in an OAuth Application
     * in order to create and update the client.
     *
     * @param oAuthApplicationInfo Object that needs to be converted.
     * @return JSON payload.
     */
    private Auth0ClientInfo createClientInfoFromOauthApplicationInfo(OAuthApplicationInfo oAuthApplicationInfo) {
        Auth0ClientInfo clientInfo = new Auth0ClientInfo();
        String userId = (String) oAuthApplicationInfo.getParameter(ApplicationConstants.
                OAUTH_CLIENT_USERNAME);
        String userNameForSp = MultitenantUtils.getTenantAwareUsername(userId);
        String domain = UserCoreUtil.extractDomainFromName(userNameForSp);
        if (domain != null && !domain.isEmpty() && !UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME.equals(domain)) {
            userNameForSp = userNameForSp.replace(UserCoreConstants.DOMAIN_SEPARATOR, "_");
        }
        String applicationName = oAuthApplicationInfo.getClientName();
        String keyType = (String) oAuthApplicationInfo.getParameter(ApplicationConstants.APP_KEY_TYPE);
        String callBackURL = oAuthApplicationInfo.getCallBackURL();
        if (keyType != null) {
            applicationName = userNameForSp.concat(applicationName).concat("_").concat(keyType);
        }
        List<String> grantTypes = new ArrayList<>();
        if (oAuthApplicationInfo.getParameter(APIConstants.JSON_GRANT_TYPES) != null) {
            grantTypes = Arrays.asList(((String) oAuthApplicationInfo.getParameter(APIConstants.JSON_GRANT_TYPES))
                    .split(","));
        }
        clientInfo.setGrantTypes(grantTypes);
        clientInfo.setClientName(applicationName);
        if (StringUtils.isNotEmpty(callBackURL)) {
            String[] calBackUris = callBackURL.split(",");
            clientInfo.setRedirectUris(Arrays.asList(calBackUris));
        }
        Object parameter = oAuthApplicationInfo.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
        Map<String, Object> additionalProperties = new HashMap<>();
        if (parameter instanceof String) {
            additionalProperties = new Gson().fromJson((String) parameter, Map.class);
        }
        if (additionalProperties.containsKey((Auth0Constants.APP_TYPE))) {
            clientInfo.setApplicationType((String) additionalProperties.get((Auth0Constants.APP_TYPE)));
        } else {
            clientInfo.setApplicationType(Auth0Constants.DEFAULT_CLIENT_APPLICATION_TYPE);
        }
        if (additionalProperties.containsKey(Auth0Constants.TOKEN_ENDPOINT_AUTH_METHOD)) {
            clientInfo.setTokenEndpointAuthMethod((String)
                    additionalProperties.get(Auth0Constants.TOKEN_ENDPOINT_AUTH_METHOD));
        }
        return clientInfo;
    }

    @Override
    public OAuthApplicationInfo updateApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {
        OAuthApplicationInfo oAuthApplicationInfo = oAuthAppRequest.getOAuthApplicationInfo();
        Auth0ClientInfo clientInfo = createClientInfoFromOauthApplicationInfo(oAuthApplicationInfo);
        clientInfo.setClientSecret(oAuthApplicationInfo.getClientSecret());
        Auth0ClientInfo createdApplication = auth0DCRClient.updateApplication(oAuthApplicationInfo.getClientId(),
                clientInfo);
        if (createdApplication != null) {
            OAuthApplicationInfo createdOAuthApplication = createOAuthAppInfoFromResponse(createdApplication);
            String audience = getAudienceFromAuthAppRequest(oAuthApplicationInfo);
            Auth0ClientGrantInfo auth0ClientGrantInfo = new Auth0ClientGrantInfo(createdApplication.getClientId(),
                    audience);
            Auth0ClientGrantInfo addedClientGrant = null;
            if (!audience.isEmpty()) {
                try {
                    addedClientGrant = auth0ClientGrant.createClientGrant(auth0ClientGrantInfo);
                    if (addedClientGrant != null) {
                        return createdOAuthApplication;
                    }
                } catch (FeignException e) {
                    if (e.status() == 409) {
                        log.warn("Client grant already exists.");
                        return createdOAuthApplication;
                    }
                }
            } else {
                log.warn("Did not provide the audience");
                return createdOAuthApplication;
            }
            if (addedClientGrant == null) {
                log.warn("Error while adding the audience");
            }
            return createdOAuthApplication;
        }
        return null;
    }

    @Override
    public void deleteApplication(String clientID) throws APIManagementException {
        auth0DCRClient.deleteApplication(clientID);
    }

    @Override
    public OAuthApplicationInfo retrieveApplication(String clientID) throws APIManagementException {
        Auth0ClientInfo auth0ClientInfo = auth0DCRClient.getApplication(clientID);
        OAuthApplicationInfo createdOauthApplication = createOAuthAppInfoFromResponse(auth0ClientInfo);
        return createdOauthApplication;
    }

    @Override
    public AccessTokenInfo getNewApplicationAccessToken(AccessTokenRequest accessTokenRequest)
            throws APIManagementException {
        Auth0ClientGrantInfo[] clientGrantInfos = auth0ClientGrant.getClientGrant(accessTokenRequest.getClientId());
        String audience = clientGrantInfos.length > 0 ? clientGrantInfos[0].getAudience() : "";
        String scopes = accessTokenRequest.getScope() != null && (accessTokenRequest.getScope().length > 0) ?
                String.join(" ", accessTokenRequest.getScope()) : "";
        String grantType = accessTokenRequest.getGrantType() != null ?
                accessTokenRequest.getGrantType() : Auth0Constants.GRANT_TYPE_CLIENT_CREDENTIALS;
        String basicCredentials = getEncodedCredentials(accessTokenRequest.getClientId(),
                accessTokenRequest.getClientSecret());
        Auth0AccessTokenResponse retrievedAccessTokenResponse = auth0TokenClient.getAccessToken(grantType, audience,
                scopes, basicCredentials);
        if (retrievedAccessTokenResponse != null) {
            AccessTokenInfo accessTokenInfo = new AccessTokenInfo();
            accessTokenInfo.setConsumerKey(accessTokenRequest.getClientId());
            accessTokenInfo.setConsumerSecret(accessTokenRequest.getClientSecret());
            accessTokenInfo.setAccessToken(retrievedAccessTokenResponse.getAccessToken());
            if (retrievedAccessTokenResponse.getScope() != null) {
                accessTokenInfo.setScope(retrievedAccessTokenResponse.getScope().split("\\s+"));
            }
            accessTokenInfo.setValidityPeriod(retrievedAccessTokenResponse.getExpiry());
            return accessTokenInfo;
        }
        return null;
    }

    @Override
    public String getNewApplicationConsumerSecret(AccessTokenRequest accessTokenRequest) throws APIManagementException {
        Auth0ClientInfo createdApplication = auth0DCRClient.regenerateClientSecret(accessTokenRequest.getClientId());
        return createdApplication.getClientSecret();
    }


    /**
     * This operation is not supported by Auth0
     */
    @Override
    public AccessTokenInfo getTokenMetaData(String s) throws APIManagementException {
        return null;
    }

    @Override
    public KeyManagerConfiguration getKeyManagerConfiguration() throws APIManagementException {
        return configuration;
    }

    @Override
    public OAuthApplicationInfo mapOAuthApplication(OAuthAppRequest oAuthAppRequest) throws APIManagementException {
        return oAuthAppRequest.getOAuthApplicationInfo();
    }

    @Override
    public void loadConfiguration(KeyManagerConfiguration keyManagerConfiguration) throws APIManagementException {
        configuration = keyManagerConfiguration;
        auth0TokenClient = Feign.builder().client(new OkHttpClient()).encoder(new FormEncoder())
                .decoder(new GsonDecoder()).errorDecoder(new ErrorDecoder.Default())
                .logger(new Slf4jLogger()).target(Auth0TokenClient.class,
                        (String) keyManagerConfiguration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT));
        Auth0APIKeyInterceptor auth0APIKeyInterceptor = new Auth0APIKeyInterceptor(auth0TokenClient,
                (String) keyManagerConfiguration.getParameter(Auth0Constants.CLIENT_ID),
                (String) keyManagerConfiguration.getParameter(Auth0Constants.CLIENT_SECRET),
                (String) keyManagerConfiguration.getParameter(Auth0Constants.AUDIENCE));
        String clientRegistrationEndpoint =
                ((String) keyManagerConfiguration.getParameter(Auth0Constants.AUDIENCE)).concat("clients");
        String clientGrantEndpoint =
                ((String) keyManagerConfiguration.getParameter(Auth0Constants.AUDIENCE)).concat("client-grants");
        auth0DCRClient = Feign.builder().client(new OkHttpClient()).encoder(new GsonEncoder())
                .decoder(new GsonDecoder()).errorDecoder(new ErrorDecoder.Default())
                .logger(new Slf4jLogger()).requestInterceptor(auth0APIKeyInterceptor)
                .target(Auth0DCRClient.class, clientRegistrationEndpoint);
        auth0ClientGrant = Feign.builder().client(new OkHttpClient()).encoder(new GsonEncoder())
                .decoder(new GsonDecoder()).errorDecoder(new ErrorDecoder.Default())
                .logger(new Slf4jLogger()).requestInterceptor(auth0APIKeyInterceptor)
                .target(Auth0ClientGrant.class, clientGrantEndpoint);
    }

    @Override
    public boolean registerNewResource(API api, Map map) throws APIManagementException {
        return false;
    }

    @Override
    public Map getResourceByApiId(String s) throws APIManagementException {
        return null;
    }

    @Override
    public boolean updateRegisteredResource(API api, Map map) throws APIManagementException {
        return false;
    }

    @Override
    public void deleteRegisteredResourceByAPIId(String s) throws APIManagementException {

    }

    @Override
    public void deleteMappedApplication(String s) throws APIManagementException {

    }

    @Override
    public Set<String> getActiveTokensByConsumerKey(String s) throws APIManagementException {
        return null;
    }

    @Override
    public AccessTokenInfo getAccessTokenByConsumerKey(String s) throws APIManagementException {
        return null;
    }

    @Override
    public boolean canHandleToken(String accessToken) throws APIManagementException {
        return false;
    }

    @Override
    public Map<String, Set<Scope>> getScopesForAPIS(String apiIdsString) throws APIManagementException {
        Map<String, Set<Scope>> apiToScopeMapping = new HashMap<>();
        ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
        Map<String, Set<String>> apiToScopeKeyMapping = apiMgtDAO.getScopesForAPIS(apiIdsString);
        for (String apiId : apiToScopeKeyMapping.keySet()) {
            Set<Scope> apiScopes = new LinkedHashSet<>();
            Set<String> scopeKeys = apiToScopeKeyMapping.get(apiId);
            for (String scopeKey : scopeKeys) {
                Scope scope = getScopeByName(scopeKey);
                apiScopes.add(scope);
            }
            apiToScopeMapping.put(apiId, apiScopes);
        }
        return apiToScopeMapping;
    }

    @Override
    public void registerScope(Scope scope) throws APIManagementException {

    }

    @Override
    public Scope getScopeByName(String s) throws APIManagementException {
        return null;
    }

    @Override
    public Map<String, Scope> getAllScopes() throws APIManagementException {
        return null;
    }

    @Override
    public void deleteScope(String s) throws APIManagementException {

    }

    @Override
    public void updateScope(Scope scope) throws APIManagementException {

    }

    @Override
    public boolean isScopeExists(String s) throws APIManagementException {
        return false;
    }

    @Override
    public String getType() {
        return Auth0Constants.AUTH0_TYPE;
    }
}
