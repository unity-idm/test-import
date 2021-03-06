/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

@PrototypeComponent
class OTPCredentialEditor implements CredentialEditor
{
	private MessageSource msg;
	private OTPEditorComponent editor;
	private OTPCredentialDefinition config;
	
	@Autowired
	OTPCredentialEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context) 
	{
		config = JsonUtil.parse(context.getCredentialConfiguration(), 
				OTPCredentialDefinition.class); 
		editor = new OTPEditorComponent(msg, context, config);
		return new ComponentsContainer(editor);
	}

	@Override
	public Optional<Component> getViewer(String credentialInfo)
	{
		if (Strings.isNullOrEmpty(credentialInfo))
			return Optional.empty();
		OTPExtraInfo extraInfo = JsonUtil.parse(credentialInfo, OTPExtraInfo.class);
		Label lastChange = new Label(msg.getMessage("OTPCredentialEditor.lastModification", 
				extraInfo.lastModification));
		return Optional.of(lastChange);
	}
	
	@Override
	public String getValue() throws IllegalCredentialException
	{
		return editor.getValue();
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		if (error != null)
			NotificationPopup.showError(msg, 
				msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
				error);
	}
}
