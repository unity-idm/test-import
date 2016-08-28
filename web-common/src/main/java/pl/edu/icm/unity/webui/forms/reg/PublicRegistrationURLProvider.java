/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import static pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport.REGISTRATION_FRAGMENT_PREFIX;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.wellknownurl.PublicViewProvider;

/**
 * Provides access to public registration forms via well-known links
 * @author K. Benedyczak
 */
@Component
public class PublicRegistrationURLProvider implements PublicViewProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			PublicRegistrationURLProvider.class);
	private RegistrationsManagement regMan;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private UnityMessageSource msg;
	private UnityServerConfiguration cfg;
	private IdPLoginController idpLoginController;
	private InvitationManagement invitationMan;
	private CredentialManagement credMan;
	
	@Autowired
	public PublicRegistrationURLProvider(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement regMan,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributeTypeManagement aTypeMan, 
			@Qualifier("insecure") GroupsManagement groupsMan,
			@Qualifier("insecure") InvitationManagement invitationMan,
			@Qualifier("insecure") CredentialManagement credMan,
			UnityServerConfiguration cfg, 
			IdPLoginController idpLoginController)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.aTypeMan = aTypeMan;
		this.groupsMan = groupsMan;
		this.invitationMan = invitationMan;
		this.credMan = credMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
	}

	@Override
	public String getViewName(String viewAndParameters)
	{
		if (!viewAndParameters.startsWith(REGISTRATION_FRAGMENT_PREFIX))
			return null;
		String viewName = viewAndParameters.substring(REGISTRATION_FRAGMENT_PREFIX.length());
		return getForm(viewName) == null ? null : viewAndParameters;
	}

	@Override
	public View getView(String viewName)
	{
		RegistrationForm form = getForm(viewName.substring(REGISTRATION_FRAGMENT_PREFIX.length()));
		if (form == null)
			return null;
		return new StandaloneRegistrationView(form, msg, regMan, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, invitationMan, aTypeMan,
				groupsMan, credMan, 
				cfg, idpLoginController);
	}

	
	private RegistrationForm getForm(String name)
	{
		try
		{
			List<RegistrationForm> forms = regMan.getForms();
			for (RegistrationForm regForm: forms)
				if (regForm.isPubliclyAvailable() && regForm.getName().equals(name))
					return regForm;
		} catch (EngineException e)
		{
			log.error("Can't load registration forms", e);
		}
		return null;
	}
}
