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

package org.wso2.auth0.client.model;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.auth0.client.Auth0Constants;
import org.wso2.auth0.client.Auth0OAuthClient;
import org.wso2.carbon.apimgt.api.APIManagementException;

public class Auth0APIKeyInterceptor implements RequestInterceptor {
    private static final Log log = LogFactory.getLog(Auth0APIKeyInterceptor.class);

    private Auth0AccessTokenResponse accessTokenInfo;
    private Auth0TokenClient auth0TokenClient;
    private String consumerKey;
    private String consumerSecret;
    private String audience;

    public Auth0APIKeyInterceptor(Auth0TokenClient auth0TokenClient, String consumerKey, String consumerSecret,
                                  String audience) {
        this.auth0TokenClient = auth0TokenClient;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.audience = audience;
        getAccessToken();
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (accessTokenInfo == null || (System.currentTimeMillis() >
                (accessTokenInfo.getCreatedAt() + accessTokenInfo.getExpiry() * 1000))) {
            getAccessToken();
        }
        requestTemplate.header("Authorization", "Bearer ".concat(accessTokenInfo.getAccessToken()));
    }


    /**
     * Renew the access token of the management API
     */
    private void getAccessToken() {
        try {
            String basicCredentials = Auth0OAuthClient.getEncodedCredentials(this.consumerKey, this.consumerSecret);
            Auth0AccessTokenResponse accessTokenResponse =
                    auth0TokenClient.getAccessToken(Auth0Constants.GRANT_TYPE_CLIENT_CREDENTIALS, this.audience,
                            "", basicCredentials);
            if (accessTokenResponse != null) {
                this.accessTokenInfo = accessTokenResponse;
                this.accessTokenInfo.setCreatedAt(System.currentTimeMillis());
            }
        } catch (APIManagementException e) {
            log.error("Error while encoding credentials", e);
        }
    }
}
