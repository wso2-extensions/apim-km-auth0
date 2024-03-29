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

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface Auth0ClientGrant {
    @RequestLine("GET ?client_id={client_id}")
    @Headers("Content-Type: application/json")
    public Auth0ClientGrantInfo[] getClientGrant(@Param("client_id") String clientId);

    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    public Auth0ClientGrantInfo createClientGrant(Auth0ClientGrantInfo clientGrant);

    @RequestLine("PATCH /{grantId}")
    @Headers("Content-Type: application/json")
    public Auth0ClientInfo updateClientGrant(@Param("grantId") String grantId, Auth0ClientGrantInfo clientGrant);
}
