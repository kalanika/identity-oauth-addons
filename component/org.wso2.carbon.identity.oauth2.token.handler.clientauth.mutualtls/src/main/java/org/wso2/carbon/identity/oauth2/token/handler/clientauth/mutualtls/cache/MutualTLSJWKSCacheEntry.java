/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.token.handler.clientauth.mutualtls.cache;

import com.nimbusds.jose.util.Resource;

/**
 * MutualTLSJWKSCacheEntry wraps Resource cache value to make them serializable. This will be used with MutualTLSJWKSCache.
 */
public class MutualTLSJWKSCacheEntry extends org.wso2.carbon.identity.core.cache.CacheEntry {

    private transient Resource resource;

    public MutualTLSJWKSCacheEntry(Resource resource) {
        this.resource = resource;
    }

    public Resource getValue() {
        return resource;
    }
}
