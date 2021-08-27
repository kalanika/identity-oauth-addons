/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * under the License
 */

package org.wso2.carbon.identity.dpop.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.identity.auth.service.handler.AuthenticationHandler;
import org.wso2.carbon.identity.dpop.handler.DPoPAuthenticationHandler;
import org.wso2.carbon.identity.dpop.listener.OauthDPoPInterceptorHandlerProxy;
import org.wso2.carbon.identity.dpop.token.binding.DPoPBasedTokenBinder;
import org.wso2.carbon.identity.oauth.common.token.bindings.TokenBinderInfo;
import org.wso2.carbon.identity.oauth.event.OAuthEventInterceptor;

@Component(
        name = "org.wso2.carbon.identity.oauth.dpop.listener.oauth.dpopinterceptorhandler",
        immediate = true)
public class DPoPServiceComponent {

    private static final Log log = LogFactory.getLog(DPoPServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext().registerService(OAuthEventInterceptor.class,
                    new OauthDPoPInterceptorHandlerProxy(), null);
            context.getBundleContext().registerService(AuthenticationHandler.class.getName(),
                    new DPoPAuthenticationHandler(), null);
            context.getBundleContext().registerService(TokenBinderInfo.class.getName(),
                    new DPoPBasedTokenBinder(), null);
            log.debug("DPoPService is activated.");
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }
}
