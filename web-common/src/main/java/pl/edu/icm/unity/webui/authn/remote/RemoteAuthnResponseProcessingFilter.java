/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.remote;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * Common filter to be installed on endpoints which can receive response of remote authentication.
 * It should be installed on the target endpoint, before authentication filter, as it may set up session and cookies 
 * should be set on the proper path.
 */
@PrototypeComponent
public class RemoteAuthnResponseProcessingFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, RemoteAuthnResponseProcessingFilter.class);
	public static final String CONTEXT_ID_HTTP_PARAMETER = "__remote_authn_context_id";
	public static final String DECISION_SESSION_ATTRIBUTE = "__ff_post_authn_decision";
	private final SharedRemoteAuthenticationContextStore remoteAuthnContextStore;
	private final RemoteAuthnResponseProcessor remoteAuthnResponseProcessor;
	
	public RemoteAuthnResponseProcessingFilter(SharedRemoteAuthenticationContextStore remoteAuthnContextStore,
			RemoteAuthnResponseProcessor remoteAuthnResponseProcessor)
	{
		this.remoteAuthnContextStore = remoteAuthnContextStore;
		this.remoteAuthnResponseProcessor = remoteAuthnResponseProcessor;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		String requestId = request.getParameter(CONTEXT_ID_HTTP_PARAMETER);
		if (requestId == null)
		{
			chain.doFilter(request, response);
			return;
		}
		log.debug("Processing remote authentication with context id {}", requestId);

		RemoteAuthnState authnContext;
		try
		{
			authnContext = remoteAuthnContextStore.getAuthnContext(requestId);
			log.debug("Got remote context associated with id {}", requestId);
			remoteAuthnContextStore.removeAuthnContext(requestId);
		} catch (WrongArgumentException e)
		{
			log.debug("Request with invalid remote authn context {}, ignoring it", requestId);
			chain.doFilter(request, response);
			return;
		}
		
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		PostAuthenticationStepDecision postFirstFactorDecision = remoteAuthnResponseProcessor
				.processResponse(authnContext, httpRequest, httpResponse);

		httpRequest.getSession().setAttribute(DECISION_SESSION_ATTRIBUTE, 
				new PostAuthenticationDecissionWithContext(postFirstFactorDecision, 
						authnContext.getAuthenticationTriggeringContext()));
		log.debug("Authentication result was set in session");

		httpResponse.sendRedirect(authnContext.getUltimateReturnURL());
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void destroy()
	{
	}
	
	public static class PostAuthenticationDecissionWithContext 
	{
		public final PostAuthenticationStepDecision postFirstFactorDecision;
		public final AuthenticationTriggeringContext triggeringContext;

		public PostAuthenticationDecissionWithContext(PostAuthenticationStepDecision postFirstFactorDecision,
				AuthenticationTriggeringContext triggeringContext)
		{
			this.postFirstFactorDecision = postFirstFactorDecision;
			this.triggeringContext = triggeringContext;
		}
	}
}
