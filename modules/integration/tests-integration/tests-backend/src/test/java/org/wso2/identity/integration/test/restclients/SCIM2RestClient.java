/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
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
package org.wso2.identity.integration.test.restclients;

import io.restassured.http.ContentType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.identity.integration.test.rest.api.user.common.model.GroupRequestObject;
import org.wso2.identity.integration.test.rest.api.user.common.model.PatchOperationRequestObject;
import org.wso2.identity.integration.test.rest.api.user.common.model.RoleRequestObject;
import org.wso2.identity.integration.test.rest.api.user.common.model.RoleSearchRequestObject;
import org.wso2.identity.integration.test.rest.api.user.common.model.UserObject;
import org.wso2.identity.integration.test.utils.OAuth2Constant;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SCIM2RestClient extends RestBaseClient {

    private static final String SCIM2_USERS_ENDPOINT = "scim2/Users";
    private static final String SCIM2_ROLES_ENDPOINT = "scim2/Roles";
    private static final String SCIM2_GROUPS_ENDPOINT = "scim2/Groups";
    private static final String SCIM2_SEARCH_PATH = "/.search";
    private static final String SCIM_JSON_CONTENT_TYPE = "application/scim+json";
    private static final String ROLE_SEARCH_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:SearchRequest";
    private static final String USER_ENTERPRISE_SCHEMA = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";
    private static final String SCIM_SCHEMA_EXTENSION_ENTERPRISE = "scimSchemaExtensionEnterprise";
    private static final String DISPLAY_NAME_ATTRIBUTE = "displayName";
    private static final String ATTRIBUTES_PART = "?attributes=";
    private static final String EQ_OP = "eq";
    private final String serverUrl;
    private final String tenantDomain;
    private final String username;
    private final String password;

    public SCIM2RestClient(String serverUrl, Tenant tenantInfo){

        this.serverUrl = serverUrl;
        this.tenantDomain = tenantInfo.getContextUser().getUserDomain();
        this.username = tenantInfo.getContextUser().getUserName();
        this.password = tenantInfo.getContextUser().getPassword();
    }

    /**
     * Create a user.
     *
     * @param userInfo object with user creation details.
     * @return Id of the created user.
     * @throws Exception If an error occurred while creating a group.
     */
    public String createUser(UserObject userInfo) throws Exception {

        String jsonRequest = toJSONString(userInfo);
        if (userInfo.getScimSchemaExtensionEnterprise() != null) {
            jsonRequest = jsonRequest.replace(SCIM_SCHEMA_EXTENSION_ENTERPRISE, USER_ENTERPRISE_SCHEMA);
        }

        try (CloseableHttpResponse response = getResponseOfHttpPost(getUsersPath(), jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_CREATED,
                    "User creation failed");
            JSONObject jsonResponse = getJSONObject(EntityUtils.toString(response.getEntity()));
            return jsonResponse.get("id").toString();
        }
    }

    /**
     * Create a user inside an organization.
     *
     * @param userInfo         Object with user creation details.
     * @param switchedM2MToken Switched M2M token for the given organization.
     * @return ID of the created user.
     * @throws Exception If an error occurred while creating the user.
     */
    public String createSubOrgUser(UserObject userInfo, String switchedM2MToken) throws Exception {

        String jsonRequest = toJSONString(userInfo);
        if (userInfo.getScimSchemaExtensionEnterprise() != null) {
            jsonRequest = jsonRequest.replace(SCIM_SCHEMA_EXTENSION_ENTERPRISE, USER_ENTERPRISE_SCHEMA);
        }

        try (CloseableHttpResponse response = getResponseOfHttpPost(getSubOrgUsersPath(), jsonRequest,
                getHeadersWithBearerToken(switchedM2MToken))) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_CREATED,
                    "User creation failed.");
            JSONObject jsonResponse = getJSONObject(EntityUtils.toString(response.getEntity()));
            return jsonResponse.get("id").toString();
        }
    }

    /**
     * Get the details of a user.
     *
     * @param userId    Id of the user.
     * @param attribute Requested user attributes.
     * @return JSONObject of the HTTP response.
     * @throws Exception If an error occurred while getting a user.
     */
    public JSONObject getUser(String userId, String attribute) throws Exception {

        String endPointUrl;
        if (StringUtils.isEmpty(attribute)) {
            endPointUrl = getUsersPath() + PATH_SEPARATOR + userId;
        } else {
            endPointUrl = getUsersPath() + PATH_SEPARATOR + userId + ATTRIBUTES_PART + attribute;
        }

        try (CloseableHttpResponse response = getResponseOfHttpGet(endPointUrl, getHeaders())) {
            return getJSONObject(EntityUtils.toString(response.getEntity()));
        }
    }

    /**
     * Update the details of an existing user.
     *
     * @param patchUserInfo User patch request object.
     * @param userId        Id of the user.
     * @throws IOException If an error occurred while updating a user.
     */
    public void updateUser(PatchOperationRequestObject patchUserInfo, String userId) throws IOException {

        String jsonRequest = toJSONString(patchUserInfo);
        String endPointUrl = getUsersPath() + PATH_SEPARATOR + userId;

        try (CloseableHttpResponse response = getResponseOfHttpPatch(endPointUrl, jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_OK,
                    "User update failed");
        }
    }

    /**
     * Search a user and get requested attributes.
     *
     * @param userSearchReq Json String of user search request.
     * @return JSONObject of the user search response.
     * @throws Exception If an error occurred while getting a user.
     */
    public JSONObject searchUser(String userSearchReq) throws Exception {

        String endPointUrl = getUsersPath() + SCIM2_SEARCH_PATH;

        try (CloseableHttpResponse response = getResponseOfHttpPost(endPointUrl, userSearchReq, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_OK,
                    "User search failed");
            return getJSONObject(EntityUtils.toString(response.getEntity()));
        }
    }

    /**
     * Delete an existing user.
     *
     * @param userId Id of the user.
     * @throws IOException If an error occurred while deleting a user.
     */
    public void deleteUser(String userId) throws IOException {

        String endPointUrl = getUsersPath() + PATH_SEPARATOR + userId;

        try (CloseableHttpResponse response = getResponseOfHttpDelete(endPointUrl, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_NO_CONTENT,
                    "User deletion failed");
        }
    }

    /**
     * Delete an existing user of an organization.
     *
     * @param userId           ID of the user.
     * @param switchedM2MToken Switched M2M token for the given organization.
     * @throws IOException If an error occurred while deleting the user.
     */
    public void deleteSubOrgUser(String userId, String switchedM2MToken) throws IOException {

        String endPointUrl = getSubOrgUsersPath() + PATH_SEPARATOR + userId;

        try (CloseableHttpResponse response = getResponseOfHttpDelete(endPointUrl,
                getHeadersWithBearerToken(switchedM2MToken))) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_NO_CONTENT,
                    "User deletion failed.");
        }
    }

    /**
     * Add a new role.
     *
     * @param roleInfo Role request object.
     * @return Role id.
     * @throws Exception If an error occurred while adding a role.
     */
    public String addRole(RoleRequestObject roleInfo) throws Exception {

        String jsonRequest = toJSONString(roleInfo);

        try (CloseableHttpResponse response = getResponseOfHttpPost(getRolesPath(), jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_CREATED,
                    roleInfo.getDisplayName() + " Role creation failed");
            JSONObject jsonResponse = getJSONObject(EntityUtils.toString(response.getEntity()));
            return jsonResponse.get("id").toString();
        }
    }

    /**
     * Update an existing role.
     *
     * @param patchRoleInfo Role patch request object.
     * @param roleId        Role id.
     * @throws IOException If an error occurred while updating a role.
     */
    public void updateUserRole(PatchOperationRequestObject patchRoleInfo, String roleId) throws IOException {

        String jsonRequest = toJSONString(patchRoleInfo);
        String endPointUrl = getRolesPath() + PATH_SEPARATOR + roleId;

        try (CloseableHttpResponse response = getResponseOfHttpPatch(endPointUrl, jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_OK,
                    "Role update failed");
        }
    }

    /**
     * Search and get the id of a role by the name.
     *
     * @param roleName Role name.
     * @return Role id.
     * @throws Exception If an error occurred while getting a role by name.
     */
    public String getRoleIdByName(String roleName) throws Exception {

        RoleSearchRequestObject roleSearchObj = new RoleSearchRequestObject();
        roleSearchObj.addSchemas(ROLE_SEARCH_SCHEMA);

        String filterString =  DISPLAY_NAME_ATTRIBUTE + " " + EQ_OP + " " + roleName;
        roleSearchObj.setFilter(filterString);

        String jsonRequest = toJSONString(roleSearchObj);

        try (CloseableHttpResponse response = getResponseOfHttpPost(getRolesPath() + SCIM2_SEARCH_PATH,
                jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_OK,
                    "Role search failed");
            JSONObject jsonResponse = getJSONObject(EntityUtils.toString(response.getEntity()));
            JSONObject searchResult = (JSONObject) ((JSONArray) jsonResponse.get("Resources")).get(0);

            return searchResult.get("id").toString();
        }
    }

    /**
     * Delete an existing role.
     *
     * @param roleId Role id.
     * @throws IOException If an error occurred while deleting a role.
     */
    public void deleteRole(String roleId) throws IOException {

        String endPointUrl = getRolesPath() + PATH_SEPARATOR + roleId;

        try (CloseableHttpResponse response = getResponseOfHttpDelete(endPointUrl, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_NO_CONTENT,
                    "Role deletion failed");
        }
    }

    /**
     * Add a new group.
     *
     * @param groupInfo Group request object.
     * @return Group id.
     * @throws Exception If an error occurred while adding a group.
     */
    public String createGroup(GroupRequestObject groupInfo) throws Exception {
        String jsonRequest = toJSONString(groupInfo);

        try (CloseableHttpResponse response = getResponseOfHttpPost(getGroupsPath(), jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_CREATED,
                    groupInfo.getDisplay() + " Group creation failed");
            JSONObject jsonResponse = getJSONObject(EntityUtils.toString(response.getEntity()));
            return jsonResponse.get("id").toString();
        }
    }

    /**
     * Delete an existing group.
     *
     * @param groupId Group id.
     * @throws IOException If an error occurred while deleting a group.
     */
    public void deleteGroup(String groupId) throws IOException {

        String endPointUrl = getGroupsPath() + PATH_SEPARATOR + groupId;

        try (CloseableHttpResponse response = getResponseOfHttpDelete(endPointUrl, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_NO_CONTENT,
                    "Group deletion failed");
        }
    }

    private Header[] getHeaders() {

        Header[] headerList = new Header[3];
        headerList[0] = new BasicHeader(USER_AGENT_ATTRIBUTE, OAuth2Constant.USER_AGENT);
        headerList[1] = new BasicHeader(AUTHORIZATION_ATTRIBUTE, BASIC_AUTHORIZATION_ATTRIBUTE +
                Base64.encodeBase64String((username + ":" + password).getBytes()).trim());
        headerList[2] = new BasicHeader(CONTENT_TYPE_ATTRIBUTE, SCIM_JSON_CONTENT_TYPE);

        return headerList;
    }

    private Header[] getHeadersWithBearerToken(String accessToken) {

        Header[] headerList = new Header[3];
        headerList[0] = new BasicHeader(USER_AGENT_ATTRIBUTE, OAuth2Constant.USER_AGENT);
        headerList[1] = new BasicHeader(AUTHORIZATION_ATTRIBUTE, BEARER_TOKEN_AUTHORIZATION_ATTRIBUTE +
                accessToken);
        headerList[2] = new BasicHeader(CONTENT_TYPE_ATTRIBUTE, String.valueOf(ContentType.JSON));

        return headerList;
    }

    private String getUsersPath() {

        if (tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            return serverUrl + SCIM2_USERS_ENDPOINT;
        } else {
            return serverUrl + TENANT_PATH + tenantDomain + PATH_SEPARATOR + SCIM2_USERS_ENDPOINT;
        }
    }

    private String getSubOrgUsersPath() {

        if (tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            return serverUrl + ORGANIZATION_PATH + SCIM2_USERS_ENDPOINT;
        }
        return serverUrl + TENANT_PATH + tenantDomain + PATH_SEPARATOR + ORGANIZATION_PATH +SCIM2_USERS_ENDPOINT;
    }

    private String getRolesPath() {

        if (tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            return serverUrl + SCIM2_ROLES_ENDPOINT;
        } else {
            return serverUrl + TENANT_PATH + tenantDomain + PATH_SEPARATOR + SCIM2_ROLES_ENDPOINT;
        }
    }

    private String getGroupsPath() {

        if (tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            return serverUrl + SCIM2_GROUPS_ENDPOINT;
        } else {
            return serverUrl + TENANT_PATH + tenantDomain + PATH_SEPARATOR + SCIM2_GROUPS_ENDPOINT;
        }
    }

    /**
     * Close the HTTP client.
     *
     * @throws IOException If an error occurred while closing the Http Client.
     */
    public void closeHttpClient() throws IOException {

        client.close();
    }
}
