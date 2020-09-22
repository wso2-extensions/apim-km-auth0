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

import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.model.ConfigurationDto;
import org.wso2.carbon.apimgt.api.model.KeyManagerConnectorConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component(
        name = "auth0.configuration.component",
        immediate = true,
        service = KeyManagerConnectorConfiguration.class
)
public class Auth0ConnectorConfiguration implements KeyManagerConnectorConfiguration {
    @Override
    public String getImplementation() {
        return Auth0OAuthClient.class.getName();
    }

    @Override
    public String getJWTValidator() {
        return null;
    }

    @Override
    public List<ConfigurationDto> getConnectionConfigurations() {
        List<ConfigurationDto> configurationDtoList = new ArrayList<>();
        configurationDtoList
                .add(new ConfigurationDto(Auth0Constants.CLIENT_ID, "Client ID", "input",
                        "Client ID of Service Application", "", true,
                        false, Collections.emptyList(), false));
        configurationDtoList
                .add(new ConfigurationDto(Auth0Constants.CLIENT_SECRET, "Client Secret", "input",
                        "Client Secret of Service Application", "", true,
                        true, Collections.emptyList(), false));
        configurationDtoList.add(new ConfigurationDto(Auth0Constants.AUDIENCE, "Audience", "input",
                "Audience of the Admin API", "https://[tenant].[region].auth0.com/api/v2/",
                true, false, Collections.emptyList(), false));
        return configurationDtoList;
    }

    @Override
    public List<ConfigurationDto> getApplicationConfigurations() {
        List<ConfigurationDto> configurationDtoList = new ArrayList<>();
        configurationDtoList.add(new ConfigurationDto("app_type", "Application Type", "select",
                "Type of the application to create", "regular_web", false,
                false, Arrays.asList("regular_web", "native", "spa", "non_interactive"), false));
        configurationDtoList.add(new ConfigurationDto("token_endpoint_auth_method",
                "Token Endpoint Authentication Method", "select",
                "How to Authenticate Token Endpoint", "client_secret_basic", true,
                true, Arrays.asList("client_secret_basic", "client_secret_post"), false));
        configurationDtoList.add(new ConfigurationDto("audience_of_api", "Audience of the API",
                "text", "The audience of the API which intended to use this application", "",
                true, false, Collections.emptyList(), false));
        return configurationDtoList;
    }

    @Override
    public String getType() {
        return Auth0Constants.AUTH0_TYPE;
    }

    @Override
    public String getDisplayName() {
        return Auth0Constants.AUTH0_DISPLAY_NAME;
    }

    @Override
    public String getDefaultScopesClaim() {
        return Auth0Constants.SCOPE;
    }

    @Override
    public String getDefaultConsumerKeyClaim() {
        return Auth0Constants.AZP;
    }
}
