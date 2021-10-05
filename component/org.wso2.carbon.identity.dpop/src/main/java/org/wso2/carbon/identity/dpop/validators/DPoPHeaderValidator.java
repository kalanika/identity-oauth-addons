/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.dpop.validators;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.dpop.constant.DPoPConstants;
import org.wso2.carbon.identity.dpop.token.binder.DPoPBasedTokenBinder;
import org.wso2.carbon.identity.dpop.util.Utils;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2ClientException;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.model.HttpRequestHeader;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.bindings.TokenBinding;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

/**
 * DPoP Header  validator.
 */
public class DPoPHeaderValidator {

    static final Log log = LogFactory.getLog(DPoPHeaderValidator.class);

    public static boolean isValidDPoPProof(Object request, String dPoPProof) throws ParseException, IdentityOAuth2Exception {

        SignedJWT signedJwt = SignedJWT.parse(dPoPProof);
        JWSHeader header = signedJwt.getHeader();

        if (validateDPoPPayload(request, signedJwt.getJWTClaimsSet()) && validateDPoPHeader(header)) {
            return true;
        }
        return false;
    }


    public static boolean isValidDPoP(String dPoPProof, OAuth2AccessTokenReqDTO tokenReqDTO,
                                      OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        try {
            HttpServletRequest request = tokenReqDTO.getHttpServletRequestWrapper();

            if(isValidDPoPProof(request,dPoPProof) &&
            (StringUtils.isNotBlank(Utils.getThumbprintOfKeyFromDpopProof(dPoPProof)))){
                TokenBinding tokenBinding = new TokenBinding();
                tokenBinding.setBindingType(DPoPConstants.DPOP_TOKEN_TYPE);
                String thumbprint = Utils.getThumbprintOfKeyFromDpopProof(dPoPProof);
                tokenBinding.setBindingValue(thumbprint);
                tokenBinding.setBindingReference(DigestUtils.md5Hex(thumbprint));
                DPoPBasedTokenBinder.setTokenBindingValue(tokenBinding.getBindingValue());
                tokReqMsgCtx.setTokenBinding(tokenBinding);
                setCnFValue(tokReqMsgCtx, tokenBinding.getBindingReference());
                return true;
            }
        } catch (ParseException e) {
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }
        return false;
    }

    private static boolean validateDPoPHeader(JWSHeader header) throws IdentityOAuth2Exception {

        if (checkJwk(header) && checkAlg(header) && checkHeaderType(header)) {
            return true;
        }
        return false;
    }

    private static boolean validateDPoPPayload(Object request, JWTClaimsSet jwtClaimsSet)
            throws IdentityOAuth2Exception, ParseException {

        if (checkJwtClaimSet(jwtClaimsSet) && checkDPoPHeaderValidity(jwtClaimsSet) && checkJti(jwtClaimsSet) &&
                checkHTTPMethod(request, jwtClaimsSet) && checkHTTPURI(request, jwtClaimsSet)) {
            return true;
        }
        return false;
    }

    private static boolean checkJwk(JWSHeader header) throws IdentityOAuth2ClientException {

        if (header.getJWK() == null) {
            if (log.isDebugEnabled()) {
                log.debug("'jwk' is not presented in the DPoP Proof header");
            }
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }
        return true;
    }

    private static boolean checkAlg(JWSHeader header) throws IdentityOAuth2ClientException {

        JWSAlgorithm algorithm = header.getAlgorithm();
        if (algorithm == null) {
            if (log.isDebugEnabled()) {
                log.debug("'algorithm' is not presented in the DPoP Proof header");
            }
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }
        return true;
    }

    private static boolean checkHeaderType(JWSHeader header) throws IdentityOAuth2ClientException {

        if (!DPoPConstants.DPOP_JWT_TYPE.equalsIgnoreCase(header.getType().toString())) {
            if (log.isDebugEnabled()) {
                log.debug(" typ field value in the DPoP Proof header  is not equal to 'dpop+jwt'");
            }
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }

        return true;
    }

    private static boolean checkJwtClaimSet(JWTClaimsSet jwtClaimsSet) throws IdentityOAuth2ClientException {

        if (jwtClaimsSet == null) {
            log.debug("'jwtClaimsSet' is missing in the body of a DPoP proof.");
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }
        return true;
    }

    private static boolean checkDPoPHeaderValidity(JWTClaimsSet jwtClaimsSet) throws IdentityOAuth2ClientException {

        Timestamp currentTimestamp = new Timestamp(new Date().getTime());
        Date issuedAt = (Date) jwtClaimsSet.getClaim(DPoPConstants.DPOP_ISSUED_AT);
        if (issuedAt == null) {
            log.debug("DPoP Proof missing the 'iat' field.");
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }

        boolean isExpired = (currentTimestamp.getTime() - issuedAt.getTime()) > getDPoPValidityPeriod();
        if (isExpired) {
            String error = "Expired DPoP Proof";
            log.debug(error);
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, error);
        }
        return true;

    }

    private static boolean checkJti(JWTClaimsSet jwtClaimsSet) throws IdentityOAuth2ClientException {

        if (!jwtClaimsSet.getClaims().containsKey(DPoPConstants.JTI)) {
            log.debug("'jti' is missing in the 'jwtClaimsSet' of the DPoP proof body.");
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }
        return true;
    }

    private static boolean checkHTTPMethod(Object request, JWTClaimsSet jwtClaimsSet) throws IdentityOAuth2ClientException {

        Object dPoPHttpMethod = jwtClaimsSet.getClaim(DPoPConstants.DPOP_HTTP_METHOD);

        // Validate if the DPoP proof HTTP method matches that of the request.
        if (!((HttpServletRequest) request).getMethod().equalsIgnoreCase(dPoPHttpMethod.toString())) {
            log.debug("DPoP Proof HTTP method mismatch.");
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }
        return true;
    }

    private static boolean checkHTTPURI(Object request, JWTClaimsSet jwtClaimsSet) throws IdentityOAuth2ClientException {

        // Validate if the DPoP proof HTTP URI matches that of the request.
        Object dPoPContextPath = jwtClaimsSet.getClaim(DPoPConstants.DPOP_HTTP_URI);
        if (!((HttpServletRequest) request).getRequestURL().toString().equalsIgnoreCase(dPoPContextPath.toString())) {
            log.debug("DPoP Proof context path mismatch.");
            throw new IdentityOAuth2ClientException(DPoPConstants.INVALID_DPOP_PROOF, DPoPConstants.INVALID_DPOP_ERROR);
        }
        return true;
    }

    private static int getDPoPValidityPeriod() {

        String validityPeriod = IdentityUtil.getProperty(DPoPConstants.HEADER_VALIDITY);
        return StringUtils.isNotBlank(validityPeriod) ? Integer.parseInt(validityPeriod.trim()) * 1000
                : DPoPConstants.DEFAULT_HEADER_VALIDITY;
    }

    public static String getDPoPHeader(OAuthTokenReqMessageContext tokReqMsgCtx) {

        HttpRequestHeader[] httpRequestHeaders = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getHttpRequestHeaders();
        if (httpRequestHeaders != null) {
            for (HttpRequestHeader header : httpRequestHeaders) {
                if (header != null && DPoPConstants.OAUTH_DPOP_HEADER.equalsIgnoreCase(header.getName())) {
                    return ArrayUtils.isNotEmpty(header.getValue()) ? header.getValue()[0] : null;
                }
            }
        }
        return null;
    }

    public static String getApplicationBindingType(String consumerKey) throws InvalidOAuthClientException,
            IdentityOAuth2Exception {

        OAuthAppDO oauthAppDO = OAuth2Util.getAppInformationByClientId(consumerKey);
        return oauthAppDO.getTokenBindingType();
    }

    private static void setCnFValue(OAuthTokenReqMessageContext tokReqMsgCtx, String tokenBindingReference) {

        JSONObject obj = new JSONObject();
        obj.put(DPoPConstants.JWK_THUMBPRINT, tokenBindingReference);
        tokReqMsgCtx.addProperty(DPoPConstants.CNF, obj);
    }
}