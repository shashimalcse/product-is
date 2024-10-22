/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.identity.integration.test.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.RFC6265CookieSpecProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.identity.integration.test.application.mgt.AbstractIdentityFederationTestCase;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.AdvancedApplicationConfiguration;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.ApplicationModel;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.ApplicationResponseModel;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.AuthenticationSequence;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.AuthenticationSequence.TypeEnum;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.Authenticator;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.InboundProtocols;
import org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.OpenIDConnectConfiguration;
import org.wso2.identity.integration.test.rest.api.server.idp.v1.model.FederatedAuthenticatorRequest;
import org.wso2.identity.integration.test.rest.api.server.idp.v1.model.FederatedAuthenticatorRequest.FederatedAuthenticator;
import org.wso2.identity.integration.test.rest.api.server.idp.v1.model.IdentityProviderPOSTRequest;
import org.wso2.identity.integration.test.rest.api.server.idp.v1.model.ProvisioningRequest;
import org.wso2.identity.integration.test.rest.api.server.idp.v1.model.ProvisioningRequest.JustInTimeProvisioning;
import org.wso2.identity.integration.test.rest.api.user.common.model.ListObject;
import org.wso2.identity.integration.test.rest.api.user.common.model.PatchOperationRequestObject;
import org.wso2.identity.integration.test.rest.api.user.common.model.RoleItemAddGroupobj;
import org.wso2.identity.integration.test.rest.api.user.common.model.UserObject;
import org.wso2.identity.integration.test.restclients.SCIM2RestClient;
import org.wso2.identity.integration.test.utils.DataExtractUtil;
import org.wso2.identity.integration.test.utils.IdentityConstants;
import org.wso2.identity.integration.test.utils.OAuth2Constant;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.ADDITIONAL_DATA;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.AUTHENTICATOR;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.AUTHENTICATORS;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.AUTHENTICATOR_ID;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.AUTH_DATA_CODE;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.AUTH_DATA_SESSION_STATE;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.CONTENT_TYPE_APPLICATION_JSON;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.FLOW_ID;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.FLOW_STATUS;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.FLOW_TYPE;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.HREF;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.IDP;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.LINKS;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.METADATA;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.NEXT_STEP;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.PROMPT_TYPE;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.REDIRECT_URL;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.REQUIRED_PARAMS;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.RESPONSE_MODE;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.STATE;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.STEP_TYPE;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.SUCCESS_COMPLETED;
import static org.wso2.identity.integration.test.applicationNativeAuthentication.Constants.UTF_8;

/**
 * Integration test cases for sharing federated token when the IDP config is enabled.
 * Federated OIDC IDP set as the authenticator for the application.
 * After getting response of the authentication init request, application performs an OIDC authentication
 * in federated IDP which is similar to browser based login.
 * After successful federated login, the application will acquire code and state from federated authenticator.
 * Application will exchange those data for the auth_code from primary idp.
 */
public class FederatedTokenSharingTestCase extends AbstractIdentityFederationTestCase {

    private static final String SHARE_FEDERATED_TOKEN_CONFIG = "ShareFederatedToken";
    private static final String FEDERATED_TOKEN_ALLOWED_SCOPE = "FederatedTokenAllowedScope";
    private static final String SHARE_FEDERATED_TOKEN_PARAM = "share_federated_token";
    private static final String FEDERATED_TOKEN_SCOPE = "federated_token_scope";

    private static final String PRIMARY_IS_SP_NAME = "travelocity";
    private static final String PRIMARY_IS_IDP_NAME = "trustedIdP";
    private static final String PRIMARY_IS_IDP_AUTHENTICATOR_NAME_OIDC = "OpenIDConnectAuthenticator";
    // AUTHENTICATOR_ID is base64URLEncode of PRIMARY_IS_IDP_AUTHENTICATOR_NAME_OIDC:PRIMARY_IS_IDP_NAME
    private static final String FEDERATE_AUTHENTICATOR_ID = "T3BlbklEQ29ubmVjdEF1dGhlbnRpY2F0b3I6dHJ1c3RlZElkUA";
    private static final String ENCODED_PRIMARY_IS_IDP_AUTHENTICATOR_ID_OIDC = "T3BlbklEQ29ubmVjdEF1dGhlbnRpY2F0b3I";
    private static final String PRIMARY_IS_IDP_CALLBACK_URL = "https://localhost:9853/commonauth";
    private static final String PRIMARY_IS_TOKEN_URL = "https://localhost:9853/oauth2/token";
    private static final String SECONDARY_IS_TEST_USERNAME = "testFederatedUser";
    private static final String SECONDARY_IS_TEST_PASSWORD = "TestFederatePassword@123";
    private static final String SECONDARY_IS_TEST_USER_ROLES = "admin";
    private static final String SECONDARY_IS_SP_NAME = "secondarySP";
    private static final String SECONDARY_IS_IDP_CALLBACK_URL = "https://localhost:9854/commonauth";
    private static final String SECONDARY_IS_TOKEN_ENDPOINT = "https://localhost:9854/oauth2/token";
    private static final String SECONDARY_IS_LOGOUT_ENDPOINT = "https://localhost:9854/oidc/logout";
    private static final String SECONDARY_IS_AUTHORIZE_ENDPOINT = "https://localhost:9854/oauth2/authorize";
    private static final String HTTPS_LOCALHOST_SERVICES = "https://localhost:%s/";
    private static final String TRUE = "true";
    private static final String SCOPES_APPROVED_FOR_TOKEN_SHARING =
            "https://www.googleapis.com/auth/calendar.readonly https://www.googleapis.com/auth/calendar";
    private static final String SCOPES_REQUESTED_FOR_TOKEN_SHARING =
            "https://www.googleapis.com/auth/calendar.readonly";
    private String secondaryISAppId;
    private String secondaryISClientID;
    private String secondaryISClientSecret;
    private String appClientID;
    private String appClientSecret;
    private String username;
    private String userPassword;
    private AutomationContext context;

    private String flowId;
    private String flowStatus;
    private String authenticatorId;
    private String href;
    private String redirectURL;
    private String nonce;
    private String state;
    private String code;
    private static final int PORT_OFFSET_0 = 0;
    private static final int PORT_OFFSET_1 = 1;
    CookieStore cookieStore;
    private CloseableHttpClient client;
    private String primaryISIdpId;
    private String primaryISAppId;
    private SCIM2RestClient scim2RestClient;
    private String secondaryISUserId;

    @DataProvider(name = "configProvider")
    public static Object[][] configProvider() {

        return new Object[][]{{TestUserMode.SUPER_TENANT_ADMIN}};
    }

    @Factory(dataProvider = "configProvider")
    public FederatedTokenSharingTestCase(TestUserMode userMode) throws Exception {

        context = new AutomationContext("IDENTITY", userMode);
        this.username = context.getContextTenant().getTenantAdmin().getUserName();
        this.userPassword = context.getContextTenant().getTenantAdmin().getPassword();
    }

    @BeforeClass(alwaysRun = true)
    public void initTest() throws Exception {

        super.initTest();

        createServiceClients(PORT_OFFSET_0, new IdentityConstants.ServiceClientType[]{
                IdentityConstants.ServiceClientType.APPLICATION_MANAGEMENT,
                IdentityConstants.ServiceClientType.IDENTITY_PROVIDER_MGT});

        createServiceClients(PORT_OFFSET_1, new IdentityConstants.ServiceClientType[]{
                IdentityConstants.ServiceClientType.APPLICATION_MANAGEMENT});

        createApplicationInSecondaryIS();//Create application in Google and obtain the clientID and the clientSecret
        createIDPInPrimaryIS();//Google IDP in IS
        createApplicationInPrimaryIS();// CallMeName app in IS

        scim2RestClient = new SCIM2RestClient(getSecondaryISURI(), tenantInfo);
        addUserToSecondaryIS();
    }

    @BeforeMethod(alwaysRun = true)
    public void initTestRun() {

        cookieStore = new BasicCookieStore();
        Lookup<CookieSpecProvider> cookieSpecRegistry = RegistryBuilder.<CookieSpecProvider>create()
                .register(CookieSpecs.DEFAULT, new RFC6265CookieSpecProvider()).build();
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
        client = HttpClientBuilder.create().setDefaultCookieSpecRegistry(cookieSpecRegistry)
                .setDefaultRequestConfig(requestConfig).setDefaultCookieStore(cookieStore).build();
    }

    @AfterClass(alwaysRun = true)
    public void endTest() throws Exception {

        try {
            deleteApplication(PORT_OFFSET_0, primaryISAppId);
            deleteIdp(PORT_OFFSET_0, primaryISIdpId);
            deleteApplication(PORT_OFFSET_1, secondaryISAppId);

            deleteAddedUsersInSecondaryIS();
            // Nullifying attributes.
            secondaryISAppId = null;
            secondaryISClientID = null;
            secondaryISClientSecret = null;
            appClientID = null;
            appClientSecret = null;
            username = null;
            userPassword = null;
            context = null;

            // Application Native Authentication related attributes
            flowId = null;
            flowStatus = null;
            authenticatorId = null;
            href = null;
            redirectURL = null;
            nonce = null;
            state = null;
            code = null;

            client.close();
            scim2RestClient.closeHttpClient();
        } catch (Exception e) {
            log.error("Failure occured due to :" + e.getMessage(), e);
            throw e;
        }
    }

    @DataProvider(name = "federatedTokenSharingParams")
    public static Object[][] federatedTokenSharingParams() {

        return new Object[][]{
                {" when IDP config is enabled, application requested the federated token", "true"},
                {" when IDP config is enabled, application did not request the federated token", "false"}
        };
    }

    @Test(groups = "wso2.is", description = "Send init authorize POST request to primary IDP.", dataProvider = "federatedTokenSharingParams")
    public void testFederatedTokenSharing(String errorMessage,
                                          String shareTokenQueryParameter) throws Exception {

        boolean applicationRequestedShareFederatedToken = Boolean.parseBoolean(shareTokenQueryParameter);

        // Send init authorize POST request to primary IDP
        authorizePrimaryIDP(shareTokenQueryParameter, errorMessage);

        // Send authorization request to federated IDP and retrieve code.
        authorizeFederatedIDP();

        // Send authentication POST request with code and state retrieved from federated IDP
        authenticatePrimaryIDPWithFederatedResponse();

        // Send get access token request.
        TokenRequest tokenReq = getTokenRequest();

        HTTPResponse tokenHTTPResp = tokenReq.toHTTPRequest().send();
        Assert.assertNotNull(tokenHTTPResp, "Access token http response is null." + errorMessage);

        TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp);
        Assert.assertNotNull(tokenResponse, "Access token response is null." + errorMessage);

        Assert.assertFalse(tokenResponse instanceof TokenErrorResponse,
                "Access token response contains errors." + errorMessage);

        OIDCTokenResponse oidcTokenResponse = (OIDCTokenResponse) tokenResponse;
        OIDCTokens oidcTokens = oidcTokenResponse.getOIDCTokens();
        Assert.assertNotNull(oidcTokens, "OIDC Tokens object is null." + errorMessage);

        String idToken = oidcTokens.getIDTokenString();
        Assert.assertNotNull(idToken, "ID token is null" + errorMessage);

        Map<String, Object> customParameters = oidcTokenResponse.getCustomParameters();
        if (applicationRequestedShareFederatedToken) {

            Assert.assertNotEquals(customParameters, Collections.emptyMap(),
                    "The OIDC token response custom parameters are empty." + errorMessage);
            Assert.assertNotNull(customParameters.get("federated_tokens"),
                    "The OIDC token response does not have federated tokens." + errorMessage);

            String federatedTokensJSONString = customParameters.get("federated_tokens").toString();
            Assert.assertTrue(StringUtils.isNotEmpty(federatedTokensJSONString),
                    "Federated token JSON string is empty." + errorMessage);

            ObjectMapper objectMapper = new ObjectMapper();
            Map[] federatedTokensArray;
            try {
                federatedTokensArray = objectMapper.readValue(federatedTokensJSONString, Map[].class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            // Iterate over each map in the array
            for (Map federatedTokenEntry : federatedTokensArray) {
                // Print each key-value pair in the map
                Assert.assertNotNull(federatedTokenEntry.get("accessToken"),
                        "Federated token does not have the accessToken value." + errorMessage);
                Assert.assertNotNull(federatedTokenEntry.get("idp"),
                        "Federated token does not have the idp value." + errorMessage);
            }
        } else {
            Assert.assertNull(customParameters.get("federated_tokens"),
                    "The OIDC token response have federated tokens" + errorMessage);
        }
    }

    private TokenRequest getTokenRequest() throws URISyntaxException {

        ClientID clientID = new ClientID(appClientID);
        Secret clientSecret = new Secret(appClientSecret);
        ClientSecretBasic clientSecretBasic = new ClientSecretBasic(clientID, clientSecret);

        URI callbackURI = new URI(PRIMARY_IS_IDP_CALLBACK_URL);
        AuthorizationCode authorizationCode = new AuthorizationCode(code);
        AuthorizationCodeGrant authorizationCodeGrant = new AuthorizationCodeGrant(authorizationCode, callbackURI);

        return new TokenRequest(new URI(PRIMARY_IS_TOKEN_URL), clientSecretBasic,
                authorizationCodeGrant);
    }

    private void authorizePrimaryIDP(String shareTokenQueryParameter, String errorMessage)
            throws IOException, ParseException, URISyntaxException {

        HttpResponse primaryISAuthorizePOSTResponse =
                sendPostRequestWithParameters(client, buildOAuth2Parameters(appClientID,
                        PRIMARY_IS_IDP_CALLBACK_URL, shareTokenQueryParameter), OAuth2Constant.AUTHORIZE_ENDPOINT_URL);
        Assert.assertNotNull(primaryISAuthorizePOSTResponse,
                "Authorization request failed. Authorized response is null." + errorMessage);

        JSONObject json = getJsonObject(primaryISAuthorizePOSTResponse);
        Assert.assertNotNull(json, "Client Native Authentication Init response is null." + errorMessage);

        validInitClientNativeAuthnResponse(json);
    }

    private void authenticatePrimaryIDPWithFederatedResponse() {

        String body = generateAuthReqBody();

        Response primaryISAuthenticationResponse = getResponseOfJSONPost(href, body, new HashMap<>());
        ExtractableResponse<Response> extractableResponse = primaryISAuthenticationResponse.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .and()
                .assertThat()
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .extract();
        Assert.assertNotNull(extractableResponse, "Federated Authentication failed. Authentication response is null.");

        validateAuthenticationResponseBody(extractableResponse);
    }

    private void authorizeFederatedIDP() throws IOException, URISyntaxException {

        HttpResponse federatedIDPAuthorizeResponse =
                sendPostRequestWithParameters(client, getNameValuePairsForExternalFederation(),
                        SECONDARY_IS_AUTHORIZE_ENDPOINT);
        Assert.assertNotNull(federatedIDPAuthorizeResponse,
                "Authorization request failed. Authorized response is null.");

        validateSecondaryISFederationResponse(federatedIDPAuthorizeResponse);
    }

    /**
     * Invoke given endpointUri for JSON POST request with given body, headers and Basic authentication, authentication
     * credential being the authenticatingUserName and authenticatingCredential.
     *
     * @param endpointUri endpoint to be invoked
     * @param body        payload
     * @param headers     list of headers to be added to the request
     * @return response
     */
    protected Response getResponseOfJSONPost(String endpointUri, String body, Map<String, String> headers) {

        return given()
                .contentType(ContentType.JSON)
                .headers(headers)
                .body(body)
                .when()
                .post(endpointUri);
    }

    /**
     * Validates specific fields in the JSON response of a basic authentication response.
     *
     * @param extractableResponse The ExtractableResponse containing the JSON response
     */
    private void validateAuthenticationResponseBody(ExtractableResponse<Response> extractableResponse) {

        // Validate specific fields in the JSON response
        flowStatus = extractableResponse
                .jsonPath()
                .getString(FLOW_STATUS);
        Assert.assertEquals(flowStatus, SUCCESS_COMPLETED);

        code = extractableResponse
                .jsonPath()
                .getString(AUTH_DATA_CODE);
        Assert.assertNotNull(code, "Authorization Code is null in the authData");

        Assert.assertNotNull(extractableResponse
                .jsonPath()
                .getString(AUTH_DATA_SESSION_STATE), "Session state is null in the authData");
    }

    /**
     * Extract the location header value from a HttpResponse.
     *
     * @param response HttpResponse object that needs the header extracted.
     * @return String value of the location header.
     */
    private String getLocationHeaderValue(HttpResponse response) {

        Header location = response.getFirstHeader(OAuth2Constant.HTTP_RESPONSE_HEADER_LOCATION);
        Assert.assertNotNull(location, "Location header is null.");
        return location.getValue();
    }

    /**
     * Send approval post request
     *
     * @param client                - http client
     * @param sessionDataKeyConsent - session consent data
     * @return http response
     * @throws java.io.IOException     java.io.IOException
     */
    private HttpResponse sendApprovalPost(HttpClient client, String sessionDataKeyConsent)
            throws IOException {

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("sessionDataKey", sessionDataKeyConsent));

        return sendPostRequestWithParameters(client, urlParameters,
                getTenantQualifiedURL(SECONDARY_IS_AUTHORIZE_ENDPOINT, tenantInfo.getDomain()));
    }

    private void addUserToSecondaryIS() throws Exception {

        UserObject user = new UserObject()
                .userName(SECONDARY_IS_TEST_USERNAME)
                .password(SECONDARY_IS_TEST_PASSWORD);

        secondaryISUserId = scim2RestClient.createUser(user);
        Assert.assertNotNull(secondaryISUserId, "User creation failed in secondary IS.");

        RoleItemAddGroupobj rolePatchReqObject = new RoleItemAddGroupobj();
        rolePatchReqObject.setOp(RoleItemAddGroupobj.OpEnum.ADD);
        rolePatchReqObject.setPath("users");
        rolePatchReqObject.addValue(new ListObject().value(secondaryISUserId));

        String adminRoleId = scim2RestClient.getRoleIdByName(SECONDARY_IS_TEST_USER_ROLES);
        scim2RestClient.updateUserRole(new PatchOperationRequestObject().addOperations(rolePatchReqObject),
                adminRoleId);
    }

    private void deleteAddedUsersInSecondaryIS() throws IOException {

        scim2RestClient.deleteUser(secondaryISUserId);
    }

    protected String getSecondaryISURI() {

        return String.format(HTTPS_LOCALHOST_SERVICES, DEFAULT_PORT + PORT_OFFSET_1);
    }

    private void createApplicationInPrimaryIS() throws Exception {

        ApplicationModel applicationCreationModel = new ApplicationModel()
                .name(PRIMARY_IS_SP_NAME)
                .description("This is a test Service Provider")
                .isManagementApp(true)
                .inboundProtocolConfiguration(new InboundProtocols().oidc(getAppOIDCConfigurations()))
                .authenticationSequence(new AuthenticationSequence()
                        .type(TypeEnum.USER_DEFINED)
                        .addStepsItem(
                                new org.wso2.identity.integration.test.rest.api.server.application.management.v1.model.AuthenticationStep()
                                        .id(1)
                                        .addOptionsItem(new Authenticator()
                                                .idp(PRIMARY_IS_IDP_NAME)
                                                .authenticator(PRIMARY_IS_IDP_AUTHENTICATOR_NAME_OIDC))))
                .advancedConfigurations(new AdvancedApplicationConfiguration().enableAPIBasedAuthentication(true));

        primaryISAppId = addApplication(PORT_OFFSET_0, applicationCreationModel);
        ApplicationResponseModel application = getApplication(PORT_OFFSET_0, primaryISAppId);
        Assert.assertNotNull(application, "Failed to create service provider 'travelocity' in primary IS");

        OpenIDConnectConfiguration oidcConfig = getOIDCInboundDetailsOfApplication(PORT_OFFSET_0, primaryISAppId);
        appClientID = oidcConfig.getClientId();
        Assert.assertNotNull(appClientID,
                "Failed to update service provider with inbound OIDC configs in primary IS");
        appClientSecret = oidcConfig.getClientSecret();
        Assert.assertNotNull(appClientSecret,
                "Failed to update service provider with inbound OIDC configs in primary IS");
    }

    private void createApplicationInSecondaryIS() throws Exception {

        ApplicationModel applicationCreationModel = new ApplicationModel()
                .name(SECONDARY_IS_SP_NAME)
                .description("This is a test Service Provider")
                .isManagementApp(true)
                .inboundProtocolConfiguration(new InboundProtocols().oidc(getSP2OIDCConfigurations()))
                .advancedConfigurations(
                        new AdvancedApplicationConfiguration().skipLoginConsent(true).skipLogoutConsent(true));

        secondaryISAppId = addApplication(PORT_OFFSET_1, applicationCreationModel);
        Assert.assertNotNull(secondaryISAppId, "Failed to create service provider 'secondarySP' in secondary IS");

        OpenIDConnectConfiguration oidcConfig = getOIDCInboundDetailsOfApplication(PORT_OFFSET_1, secondaryISAppId);
        secondaryISClientID = oidcConfig.getClientId();
        Assert.assertNotNull(secondaryISClientID,
                "Failed to update service provider with inbound OIDC configs in secondary IS");
        secondaryISClientSecret = oidcConfig.getClientSecret();
        Assert.assertNotNull(secondaryISClientSecret,
                "Failed to update service provider with inbound OIDC configs in secondary IS");
    }

    private void createIDPInPrimaryIS() throws Exception {

        FederatedAuthenticator authenticator = new FederatedAuthenticator()
                .authenticatorId(ENCODED_PRIMARY_IS_IDP_AUTHENTICATOR_ID_OIDC)
                .name(PRIMARY_IS_IDP_AUTHENTICATOR_NAME_OIDC)
                .isEnabled(true)
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(IdentityConstants.Authenticator.OIDC.IDP_NAME)
                        .value("oidcFedIdP"))
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(IdentityConstants.Authenticator.OIDC.CLIENT_ID)
                        .value(secondaryISClientID))
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(IdentityConstants.Authenticator.OIDC.CLIENT_SECRET)
                        .value(secondaryISClientSecret))
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(IdentityConstants.Authenticator.OIDC.OAUTH2_AUTHZ_URL)
                        .value(SECONDARY_IS_AUTHORIZE_ENDPOINT))
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(IdentityConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL)
                        .value(SECONDARY_IS_TOKEN_ENDPOINT))
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(IdentityConstants.Authenticator.OIDC.CALLBACK_URL)
                        .value(PRIMARY_IS_IDP_CALLBACK_URL))
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(IdentityConstants.Authenticator.OIDC.OIDC_LOGOUT_URL)
                        .value(SECONDARY_IS_LOGOUT_ENDPOINT))
                // Enable sharing federated token
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(SHARE_FEDERATED_TOKEN_CONFIG)
                        .value(TRUE))
                // Configuring the allowed scope for federated sharing
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key(FEDERATED_TOKEN_ALLOWED_SCOPE)
                        .value(SCOPES_APPROVED_FOR_TOKEN_SHARING))
                .addProperty(new org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Property()
                        .key("commonAuthQueryParams")
                        .value("scope=" + OAuth2Constant.OAUTH2_SCOPE_OPENID_WITH_INTERNAL_LOGIN));

        FederatedAuthenticatorRequest oidcAuthnConfig = new FederatedAuthenticatorRequest()
                .defaultAuthenticatorId(ENCODED_PRIMARY_IS_IDP_AUTHENTICATOR_ID_OIDC)
                .addAuthenticator(authenticator);

        ProvisioningRequest provision = new ProvisioningRequest()
                .jit(new JustInTimeProvisioning()
                        .isEnabled(true)
                        .userstore("PRIMARY"));

        IdentityProviderPOSTRequest idpPostRequest = new IdentityProviderPOSTRequest()
                .name(PRIMARY_IS_IDP_NAME)
                .federatedAuthenticators(oidcAuthnConfig)
                .provisioning(provision);

        primaryISIdpId = addIdentityProvider(PORT_OFFSET_0, idpPostRequest);
        Assert.assertNotNull(primaryISIdpId, "Failed to create Identity Provider 'trustedIdP' in primary IS");
    }

    private OpenIDConnectConfiguration getAppOIDCConfigurations() {

        List<String> grantTypes = new ArrayList<>();
        Collections.addAll(grantTypes, "authorization_code", "implicit", "password", "client_credentials",
                "refresh_token", "urn:ietf:params:oauth:grant-type:saml2-bearer", "iwa:ntlm");

        OpenIDConnectConfiguration oidcConfig = new OpenIDConnectConfiguration();
        oidcConfig.setGrantTypes(grantTypes);
        oidcConfig.addCallbackURLsItem(PRIMARY_IS_IDP_CALLBACK_URL);
        oidcConfig.setPublicClient(true);
        return oidcConfig;
    }

    private OpenIDConnectConfiguration getSP2OIDCConfigurations() {

        List<String> grantTypes = new ArrayList<>();
        Collections.addAll(grantTypes, "authorization_code", "implicit", "password", "client_credentials",
                "refresh_token", "urn:ietf:params:oauth:grant-type:saml2-bearer", "iwa:ntlm");

        OpenIDConnectConfiguration oidcConfig = new OpenIDConnectConfiguration();
        oidcConfig.setGrantTypes(grantTypes);
        oidcConfig.addCallbackURLsItem(PRIMARY_IS_IDP_CALLBACK_URL);
        return oidcConfig;
    }

    private HttpResponse sendLoginPost(HttpClient client, String sessionDataKey) throws IOException {

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("username", SECONDARY_IS_TEST_USERNAME));
        urlParameters.add(new BasicNameValuePair("password", SECONDARY_IS_TEST_PASSWORD));
        urlParameters.add(new BasicNameValuePair("sessionDataKey", sessionDataKey));
        log.info(">>> sendLoginPost:sessionDataKey: " + sessionDataKey);

        return sendPostRequestWithParameters(client, urlParameters, SECONDARY_IS_IDP_CALLBACK_URL);
    }

    private HttpResponse sendPostRequestWithParameters(HttpClient client, List<NameValuePair> urlParameters, String url)
            throws IOException {

        HttpPost request = new HttpPost(url);
        request.setHeader("User-Agent", OAuth2Constant.USER_AGENT);
        request.setEntity(new UrlEncodedFormEntity(urlParameters));

        return client.execute(request);
    }

    /**
     * Builds a list of OAuth 2.0 parameters required for initiating the authorization process.
     * The method constructs and returns a list of parameters necessary for initiating the OAuth 2.0 authorization process.
     *
     * @param consumerKey              The client's unique identifier in the OAuth 2.0 system
     * @param shareTokenQueryParameter Whether the application requests the federated token.
     * @return A list of NameValuePair representing the OAuth 2.0 parameters
     */
    private List<NameValuePair> buildOAuth2Parameters(String consumerKey, String callBackUrl,
                                                      String shareTokenQueryParameter) {

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(
                new BasicNameValuePair(OAuth2Constant.OAUTH2_RESPONSE_TYPE, OAuth2Constant.AUTHORIZATION_CODE_NAME));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_RESPONSE_MODE, RESPONSE_MODE));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_CLIENT_ID, consumerKey));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_REDIRECT_URI, callBackUrl));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_SCOPE,
                OAuth2Constant.OAUTH2_SCOPE_OPENID_WITH_INTERNAL_LOGIN));

        if (Boolean.parseBoolean(shareTokenQueryParameter)) {
            urlParameters.add(new BasicNameValuePair(SHARE_FEDERATED_TOKEN_PARAM, shareTokenQueryParameter));
            urlParameters.add(new BasicNameValuePair(FEDERATED_TOKEN_SCOPE, SCOPES_REQUESTED_FOR_TOKEN_SHARING));
        }
        return urlParameters;
    }

    /**
     * Validates the structure and content of a Client Native Authentication JSON response.
     * The method checks for the presence of required keys and their expected types in the provided JSON.
     * It verifies the format of the authentication flow, authenticators, metadata, and required parameters.
     * If the JSON response is not in the expected format, the method asserts failures using JUnit's Assert.fail().
     *
     * @param json The JSON object representing the Client Native Authentication response
     */
    private void validInitClientNativeAuthnResponse(JSONObject json) throws URISyntaxException {

        // Check for the presence of required keys and their expected types
        if (json.containsKey(FLOW_ID) && json.containsKey(FLOW_STATUS) && json.containsKey(FLOW_TYPE) &&
                json.containsKey(NEXT_STEP) && json.containsKey(LINKS)) {

            flowId = (String) json.get(FLOW_ID);
            flowStatus = (String) json.get(FLOW_STATUS);

            JSONObject nextStepNode = (JSONObject) json.get(NEXT_STEP);
            if (nextStepNode.containsKey(STEP_TYPE) && nextStepNode.containsKey(AUTHENTICATORS)) {

                JSONArray authenticatorsArray = (JSONArray) nextStepNode.get(AUTHENTICATORS);
                if (!authenticatorsArray.isEmpty()) {

                    JSONObject authenticator = (JSONObject) authenticatorsArray.get(0);

                    if (authenticator.containsKey(AUTHENTICATOR_ID) && authenticator.containsKey(AUTHENTICATOR) &&
                            authenticator.containsKey(IDP) && authenticator.containsKey(METADATA) &&
                            authenticator.containsKey(REQUIRED_PARAMS)) {

                        authenticatorId = (String) authenticator.get(AUTHENTICATOR_ID);
                        if (!StringUtils.equals(authenticatorId, FEDERATE_AUTHENTICATOR_ID)) {
                            Assert.fail("Miss match of authenticator id. Expected : " + FEDERATE_AUTHENTICATOR_ID +
                                    " but got : " + authenticatorId);
                        }
                        JSONObject metadataNode = (JSONObject) authenticator.get(METADATA);
                        if (metadataNode.containsKey(PROMPT_TYPE)) {

                            JSONObject additionalData = (JSONObject) metadataNode.get(ADDITIONAL_DATA);
                            if (!additionalData.containsKey(REDIRECT_URL)) {
                                Assert.fail("Content of additional data for the authenticator is null in " +
                                        "Client native authentication JSON Response.");
                            }
                            redirectURL = (String) additionalData.get(REDIRECT_URL);
                            if (StringUtils.isEmpty(redirectURL)) {
                                Assert.fail("Content of redirect url data for the authenticator is null in " +
                                        "Client native authentication JSON Response.");
                            }
                            nonce = DataExtractUtil.getParamFromURIString(redirectURL,
                                    "nonce");
                            state = DataExtractUtil.getParamFromURIString(redirectURL,
                                    "state");

                        } else {
                            Assert.fail("Params for the authenticator is null in " +
                                    "Client native authentication JSON Response.");
                        }
                    }
                } else {
                    Assert.fail("Authenticator is not expected format in Client native authentication");
                }
            } else {
                Assert.fail("Authenticators in Client native authentication JSON Response is null, " +
                        "expecting list of Authentication.");
            }
            JSONArray links = (JSONArray) json.get(LINKS);
            JSONObject link = (JSONObject) links.get(0);
            if (link.containsKey(HREF)) {
                href = link.get(HREF).toString();
            } else {
                Assert.fail("Link is not available for next step in Client native authentication JSON Response.");
            }
        } else {
            Assert.fail("Client native authentication JSON Response is not in expected format.");
        }
    }

    private JSONObject getJsonObject(HttpResponse response) throws IOException, ParseException {

        String responseString = EntityUtils.toString(response.getEntity(), UTF_8);
        EntityUtils.consume(response.getEntity());
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(responseString);
    }

    private List<NameValuePair> getNameValuePairsForExternalFederation() {

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(
                new BasicNameValuePair(OAuth2Constant.OAUTH2_RESPONSE_TYPE, OAuth2Constant.AUTHORIZATION_CODE_NAME));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_CLIENT_ID, secondaryISClientID));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_REDIRECT_URI, PRIMARY_IS_IDP_CALLBACK_URL));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_SCOPE,
                OAuth2Constant.OAUTH2_SCOPE_OPENID_WITH_INTERNAL_LOGIN));
        urlParameters.add(new BasicNameValuePair(OAuth2Constant.OAUTH2_NONCE, nonce));
        urlParameters.add(new BasicNameValuePair(STATE, state));
        return urlParameters;
    }

    private void validateSecondaryISFederationResponse(HttpResponse response) throws IOException, URISyntaxException {

        String locationValue = getLocationHeaderValue(response);
        EntityUtils.consume(response.getEntity());

        Assert.assertTrue(locationValue.contains(OAuth2Constant.SESSION_DATA_KEY),
                "sessionDataKey not found in response.");
        String sessionDataKey = DataExtractUtil.getParamFromURIString(locationValue, OAuth2Constant.SESSION_DATA_KEY);
        Assert.assertNotNull(sessionDataKey, "sessionDataKey is null.");

        response = sendLoginPost(client, sessionDataKey);
        Assert.assertNotNull(response, "Login request failed. response is null.");

        locationValue = getLocationHeaderValue(response);
        EntityUtils.consume(response.getEntity());
        sessionDataKey = DataExtractUtil.getParamFromURIString(locationValue, OAuth2Constant.SESSION_DATA_KEY);

        response = sendApprovalPost(client, sessionDataKey);
        Assert.assertNotNull(response, "Approval request failed. response is invalid.");

        locationValue = getLocationHeaderValue(response);

        code = DataExtractUtil.getParamFromURIString(locationValue, OAuth2Constant.AUTHORIZATION_CODE_NAME);
        state = DataExtractUtil.getParamFromURIString(locationValue, "state");

        Assert.assertNotNull(code, "Authorization code is null or could not be found.");
        EntityUtils.consume(response.getEntity());
    }

    private String generateAuthReqBody() {

        return "{\n" +
                "    \"flowId\": \"" + flowId + "\",\n" +
                "    \"selectedAuthenticator\": {\n" +
                "        \"authenticatorId\": \"" + authenticatorId + "\",\n" +
                "        \"params\": {\n" +
                "           \"code\": \"" + code + "\",\n" +
                "           \"state\": \"" + state + "\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }

}
