/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.AccessTokenResource;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * An integration test of the Token endpoint. The context is initialized
 * internally (i.e. the state which should be present after the client's &
 * user's interaction with the web authZ endpoint. Then the authz code is
 * exchanged for the access token and the user profile is fetched.
 * 
 * @author K. Benedyczak
 */
public class TokenEndpointTest extends DBIntegrationTestBase
{
	private static final String OAUTH_ENDP_CFG = "unity.oauth2.as.issuerUri=https://localhost:2443/oauth2\n"
			+ "unity.oauth2.as.signingCredential=MAIN\n"
			+ "unity.oauth2.as.clientsGroup=/oauth-clients\n"
			+ "unity.oauth2.as.usersGroup=/oauth-users\n"
			+ "#unity.oauth2.as.translationProfile=\n"
			+ "unity.oauth2.as.scopes.1.name=foo\n"
			+ "unity.oauth2.as.scopes.1.description=Provides access to foo info\n"
			+ "unity.oauth2.as.scopes.1.attributes.1=stringA\n"
			+ "unity.oauth2.as.scopes.1.attributes.2=o\n"
			+ "unity.oauth2.as.scopes.1.attributes.3=email\n"
			+ "unity.oauth2.as.scopes.2.name=bar\n"
			+ "unity.oauth2.as.scopes.2.description=Provides access to bar info\n"
			+ "unity.oauth2.as.scopes.2.attributes.1=c\n"
			+ "unity.oauth2.as.refreshTokenValidity=3600\n";

	public static final String REALM_NAME = "testr";

	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private PKIManagement pkiMan;
	@Autowired
	private AuthenticatorManagement authnMan;
	private Identity clientId1;
	

	@Before
	public void setup()
	{
		try
		{
			setupMockAuthn();
			clientId1 = OAuthTestUtils.createOauthClient(idsMan, attrsMan, groupsMan,
					eCredMan, "client1");
			OAuthTestUtils.createOauthClient(idsMan, attrsMan, groupsMan,
					eCredMan, "client2");

			createUser();
			AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 10, 100,
					-1, 600);
			realmsMan.addRealm(realm);
			List<AuthenticationOptionDescription> authnCfg = new ArrayList<>();
			authnCfg.add(new AuthenticationOptionDescription("Apass"));
			EndpointConfiguration config = new EndpointConfiguration(
					new I18nString("endpointIDP"), "desc", authnCfg,
					OAUTH_ENDP_CFG, REALM_NAME);
			endpointMan.deploy(OAuthTokenEndpoint.NAME, "endpointIDP", "/oauth",
					config);
			List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
			assertEquals(1, endpoints.size());

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	protected void setupMockAuthn() throws Exception
	{
		setupPasswordAuthn();

		authnMan.createAuthenticator("Apass", "password with rest-httpbasic", null, "",
				"credential1");
	}

	/**
	 * Only simple add user so the token may be added - attributes etc are
	 * loaded by the WebAuths endpoint which is skipped here.
	 * 
	 * @throws Exception
	 */
	protected void createUser() throws Exception
	{
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "userA"), "cr-pass",
				EntityState.valid, false);
	}

	@Test
	public void testCodeFlow() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(tokensMan, ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);

		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI("https://localhost:52443/oauth/userinfo"),
				(BearerAccessToken) parsedResp.getTokens().getAccessToken());
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new CustomHTTPSRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse uiHttpResponse = wrapped2.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		UserInfo ui = uiResponseS.getUserInfo();
		JWTClaimsSet claimSet = ui.toJWTClaimsSet();

		Assert.assertEquals("PL", claimSet.getClaim("c"));
		Assert.assertEquals("example@example.com", claimSet.getClaim("email"));
		Assert.assertEquals("userA", claimSet.getClaim("sub"));
	}

	@Test
	public void nonceIsReturnedInClaimSetForOIDCRequest() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId(), "nonce-VAL");
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(tokensMan, ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		OIDCTokenResponse parsedResp = OIDCTokenResponse.parse(resp2);
		JWTClaimsSet claimSet = parsedResp.getOIDCTokens().getIDToken().getJWTClaimsSet();
		assertThat(claimSet.getClaim("nonce"), is("nonce-VAL"));
	}

	@Test
	public void testClientCredentialFlow() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new ClientCredentialsGrant(), new Scope("foo"));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);

		JSONObject parsed = getTokenInfo(parsedResp.getTokens().getAccessToken());
		System.out.println(parsed);
		assertEquals("client1", parsed.get("sub"));
		assertEquals("client1", parsed.get("client_id"));
		assertEquals("foo", ((JSONArray) parsed.get("scope")).get(0));
		assertNotNull(parsed.get("exp"));
	}

	private JSONObject getTokenInfo(AccessToken token) throws Exception
	{
		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI("https://localhost:52443/oauth/tokeninfo"),
				(BearerAccessToken) token);
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new CustomHTTPSRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse httpResponse = wrapped2.send();

		JSONObject parsed = httpResponse.getContentAsJSONObject();
		return parsed;
	}

	private IdentityParam initUser(String username) throws Exception
	{
		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		groupsMan.addMemberFromParent("/oauth-users", new EntityParam(identity));
		aTypeMan.addAttributeType(new AttributeType("email", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("c", StringAttributeSyntax.ID));
		attrsMan.setAttribute(new EntityParam(identity),
				StringAttribute.of("email", "/oauth-users", "example@example.com"),
				false);
		attrsMan.setAttribute(new EntityParam(identity),
				StringAttribute.of("c", "/oauth-users", "PL"), false);
		return identity;
	}

	
	private AccessToken initExchangeToken(List<String> scopes, ClientAuthentication ca,
			ClientAuthentication ca2) throws Exception
	{
		IdentityParam identity = initUser("userA");

		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());

		ctx.setOpenIdMode(true);
		ctx.setRequestedScopes(new HashSet<>(scopes));

		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(tokensMan, ctx, identity);
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse resp2 = wrapped.send();
		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		assertNotNull(parsedResp.getTokens().getAccessToken());
		return parsedResp.getTokens().getAccessToken();
	}

	@Test
	public void shouldDenyToExchangeTokenWithWrongAudience() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchangeToken(
				Arrays.asList("bar", AccessTokenResource.EXCHANGE_SCOPE), ca, ca2);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new ExchangeGrant(GrantType.AUTHORIZATION_CODE, aToken.getValue(),
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						"client3"),
				new Scope("bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse errorResp = wrapped.send();
		Assert.assertEquals(errorResp.getStatusCode(), HTTPResponse.SC_BAD_REQUEST);
	}
	
	@Test
	public void shouldDenyToExchangeTokenWithWrongRequestedTokenType() throws Exception
	{

		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchangeToken(
				Arrays.asList("bar", AccessTokenResource.EXCHANGE_SCOPE), ca, ca2);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new ExchangeGrant(GrantType.AUTHORIZATION_CODE, aToken.getValue(),
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						"wrong",
						"client2"),
				new Scope("bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse errorResp = wrapped.send();
		Assert.assertEquals(errorResp.getStatusCode(), HTTPResponse.SC_BAD_REQUEST);
	}


	@Test
	public void shouldExchangeTokenWithIdToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));
		AccessToken aToken = initExchangeToken(Arrays.asList("openid", "foo", "bar",
				AccessTokenResource.EXCHANGE_SCOPE), ca, ca2);

		TokenRequest exchangeRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new ExchangeGrant(GrantType.AUTHORIZATION_CODE, aToken.getValue(),
						AccessTokenResource.ACCESS_TOKEN_TYPE_ID,
						AccessTokenResource.ID_TOKEN_TYPE_ID, "client2"),
				new Scope("openid foo bar"));

		HTTPRequest bare = exchangeRequest.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse exchangeResp = wrapped.send();
		AccessTokenResponse exchangeParsedResp = AccessTokenResponse.parse(exchangeResp);
		assertNotNull(exchangeParsedResp.getTokens().getAccessToken());
		assertNotNull(exchangeParsedResp.getCustomParameters().get("id_token"));
		assertEquals(exchangeParsedResp.getCustomParameters().get("issued_token_type"),
				AccessTokenResource.ACCESS_TOKEN_TYPE_ID);

		// check new token info
		JSONObject parsed = getTokenInfo(exchangeParsedResp.getTokens().getAccessToken());
		System.out.println(parsed);
		assertEquals("userA", parsed.get("sub"));
		assertEquals("client2", parsed.get("client_id"));
		assertEquals("client2", parsed.get("aud"));
		assertEquals("foo", ((JSONArray) parsed.get("scope")).get(0));
		assertNotNull(parsed.get("exp"));

	}

	private RefreshToken initRefresh(List<String> scope, ClientAuthentication ca)
			throws Exception
	{
		IdentityParam identity = initUser("userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());

		ctx.setRequestedScopes(new HashSet<>(scope));
		ctx.setOpenIdMode(true);
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(tokensMan, ctx, identity);

		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();
		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		assertNotNull(parsedResp.getTokens().getRefreshToken());
		return parsedResp.getTokens().getRefreshToken();
	}

	@Test
	public void shouldRefreshToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		JWTClaimsSet claimSet = refreshAndGetUserInfo(refreshToken, "foo bar", ca);

		Assert.assertEquals("PL", claimSet.getClaim("c"));
		Assert.assertEquals("example@example.com", claimSet.getClaim("email"));
		Assert.assertEquals("userA", claimSet.getClaim("sub"));

	}

	@Test
	public void shouldRefreshTokenWithIdToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("openid"), ca);

		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new RefreshTokenGrant(refreshToken), new Scope("openid"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse refreshResp = wrapped.send();
		AccessTokenResponse refreshParsedResp = AccessTokenResponse.parse(refreshResp);
		assertNotNull(refreshParsedResp.getTokens().getAccessToken());
		assertNotNull(refreshParsedResp.getCustomParameters().get("id_token"));
	}

	@Test
	public void shouldDenyToRefreshTokenWithWrongScope() throws Exception
	{

		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		// check wrong scope
		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new RefreshTokenGrant(refreshToken), new Scope("xx"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		Assert.assertEquals(errorResp.getStatusCode(), HTTPResponse.SC_BAD_REQUEST);
	}

	@Test
	public void shouldDenyToRefreshTokenByWrongClient() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new RefreshTokenGrant(refreshToken), new Scope("foo"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		Assert.assertEquals(errorResp.getStatusCode(), HTTPResponse.SC_BAD_REQUEST);

	}

	private JWTClaimsSet refreshAndGetUserInfo(RefreshToken token, String scopes,
			ClientAuthentication ca) throws Exception
	{
		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new RefreshTokenGrant(token), new Scope(scopes));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse refreshResp = wrapped.send();
		AccessTokenResponse refreshParsedResp = AccessTokenResponse.parse(refreshResp);
		assertNotNull(refreshParsedResp.getTokens().getAccessToken());
		return getUserInfo(refreshParsedResp.getTokens().getAccessToken());
	}

	private JWTClaimsSet getUserInfo(AccessToken accessToken) throws Exception
	{
		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI("https://localhost:52443/oauth/userinfo"),
				(BearerAccessToken) accessToken);
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new CustomHTTPSRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse uiHttpResponse = wrapped2.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		UserInfo ui = uiResponseS.getUserInfo();
		JWTClaimsSet claimSet = ui.toJWTClaimsSet();
		return claimSet;
	}

	/**
	 * 
	 * @author P.Piernik Simply ExchangeGrant for using with nimbusDS
	 */
	private static final class ExchangeGrant extends AuthorizationGrant
	{
		private String subjectToken;
		private String subjectTokenType;
		private String requestedType;
		private String audience;

		public ExchangeGrant(GrantType type, String subjectToken, String subjectTokenType,
				String requestedType, String audience)
		{
			// only for compilance, not used in toParameters method
			super(type);

			this.subjectToken = subjectToken;
			this.subjectTokenType = subjectTokenType;
			this.requestedType = requestedType;
			this.audience = audience;
		}

		@Override
		public Map<String, String> toParameters()
		{
			Map<String, String> params = new LinkedHashMap<>();
			params.put("grant_type", AccessTokenResource.EXCHANGE_GRANT);
			params.put("subject_token", subjectToken);
			params.put("subject_token_type", subjectTokenType);
			params.put("requested_token_type", requestedType);
			params.put("audience", audience);
			return params;

		}

	}

}
