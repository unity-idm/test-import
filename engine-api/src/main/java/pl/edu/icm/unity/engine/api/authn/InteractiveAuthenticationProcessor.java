/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Handles authentication results for interactive authentications (typically over some web binding).
 * Supports features like remember me etc.
 */
public interface InteractiveAuthenticationProcessor
{
	static final String UNITY_SESSION_COOKIE_PFX = "USESSIONID_";
	
	PostAuthenticationStepDecision processFirstFactorResult(AuthenticationResult result, AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails, boolean setRememberMe,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse);
	
	PostAuthenticationStepDecision processSecondFactorResult(PartialAuthnState state, 
			AuthenticationResult secondFactorResult, AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails, boolean setRememberMe,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse);

	PostAuthenticationStepDecision processRemoteRegistrationResult(AuthenticationResult result, 
			AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails, 
			HttpServletRequest httpRequest);
	
	void syntheticAuthenticate(AuthenticatedEntity authenticatedEntity,
			List<SessionParticipant> participants,
			AuthenticationOptionKey authnOptionKey,
			AuthenticationRealm realm,
			LoginMachineDetails machineDetails, boolean setRememberMe, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse);
	
	static String getSessionCookieName(String realmName)
	{
		return UNITY_SESSION_COOKIE_PFX+realmName;
	}
	
	public class PostAuthenticationStepDecision
	{
		public enum Decision {COMPLETED, GO_TO_2ND_FACTOR, ERROR, UNKNOWN_REMOTE_USER}
		
		private final Decision decision;
		
		private final UnknownRemoteUserDetail unknownRemoteUserDetail;
		private final ErrorDetail errorDetail;
		private final SecondFactorDetail secondFactorDetail;
	
		private PostAuthenticationStepDecision(Decision decision, UnknownRemoteUserDetail unknownRemoteUserDetail,
				ErrorDetail errorDetail, SecondFactorDetail secondFactorDetail)
		{
			this.decision = decision;
			this.unknownRemoteUserDetail = unknownRemoteUserDetail;
			this.errorDetail = errorDetail;
			this.secondFactorDetail = secondFactorDetail;
		}

		public static PostAuthenticationStepDecision unknownRemoteUser(UnknownRemoteUserDetail unknownRemoteUserDetail)
		{
			return new PostAuthenticationStepDecision(Decision.UNKNOWN_REMOTE_USER, unknownRemoteUserDetail, null, null);
		}

		public static PostAuthenticationStepDecision error(ErrorDetail errorDetail)
		{
			return new PostAuthenticationStepDecision(Decision.ERROR, null, errorDetail, null);
		}

		public static PostAuthenticationStepDecision goToSecondFactor(SecondFactorDetail secondFactorDetail)
		{
			return new PostAuthenticationStepDecision(Decision.GO_TO_2ND_FACTOR, null, null, secondFactorDetail);
		}

		public static PostAuthenticationStepDecision completed()
		{
			return new PostAuthenticationStepDecision(Decision.COMPLETED, null, null, null);
		}

		public Decision getDecision()
		{
			return decision;
		}

		public UnknownRemoteUserDetail getUnknownRemoteUserDetail()
		{
			if (decision != Decision.UNKNOWN_REMOTE_USER)
				throw new IllegalStateException("Unknown remote user detail not available in state: " + decision);
			return unknownRemoteUserDetail;
		}

		public ErrorDetail getErrorDetail()
		{
			if (decision != Decision.ERROR)
				throw new IllegalStateException("Error detail not available in state: " + decision);
			return errorDetail;
		}

		public SecondFactorDetail getSecondFactorDetail()
		{
			if (decision != Decision.GO_TO_2ND_FACTOR)
				throw new IllegalStateException("2nd factor detail not available in state: " + decision);
			return secondFactorDetail;
		}



		public static class UnknownRemoteUserDetail
		{
			public final UnknownRemotePrincipalResult unknownRemotePrincipal;

			public UnknownRemoteUserDetail(UnknownRemotePrincipalResult unknownRemotePrincipal)
			{
				this.unknownRemotePrincipal = unknownRemotePrincipal;
			}
		}

		public static class ErrorDetail
		{
			public final ResolvableError error;

			public ErrorDetail(ResolvableError error)
			{
				this.error = error;
			}
		}

		public static class SecondFactorDetail
		{
			public final PartialAuthnState postFirstFactorResult;

			public SecondFactorDetail(PartialAuthnState postFirstFactorResult)
			{
				this.postFirstFactorResult = postFirstFactorResult;
			}
		}
	}
	
	
}
