/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.translationProfile.wizard;

import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.ui.UI;

import io.imunity.webconsole.tprofile.TranslationProfileEditor;
import io.imunity.webconsole.tprofile.TranslationProfileEditDialog.Callback;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.webui.association.IntroStep;
import pl.edu.icm.unity.webui.association.SandboxStep;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier.AuthnResultListener;
import pl.edu.icm.unity.webui.sandbox.wizard.AbstractSandboxWizardProvider;

/**
 * Creates input profile creation wizard
 * @author K. Benedyczak
 */
public class ProfileWizardProvider extends AbstractSandboxWizardProvider
{
	private MessageSource msg;
	private Callback addCallback;
	private TranslationProfileEditor editor;

	public ProfileWizardProvider(MessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier, 
			TranslationProfileEditor editor, Callback addCallback)
	{
		super(sandboxURL, sandboxNotifier);
		this.msg = msg;
		this.editor = editor;
		this.addCallback = addCallback;
	}

	@Override
	public Wizard getWizardInstance()
	{
		final Wizard wizard = new Wizard();
		wizard.setSizeFull();
		
		final SandboxStep sandboxStep = new SandboxStep(msg, sandboxURL, wizard);
		final ProfileStep profileStep = new ProfileStep(msg, editor, addCallback);
		
		wizard.addStep(new IntroStep(msg, "Wizard.IntroStepComponent.introLabel"));
		wizard.addStep(sandboxStep);
		wizard.addStep(profileStep);
		
		//for the initial page
		openSandboxPopupOnNextButton(wizard);
		
		//and when the page is loaded with back button
		showSandboxPopupAfterGivenStep(wizard, IntroStep.class);

		addSandboxListener(new AuthnResultListener()
		{
			@Override
			public void onPartialAuthnResult(SandboxAuthnEvent event)
			{
				RemoteSandboxAuthnContext sandboxedCtx = ((RemoteSandboxAuthnContext) event.getCtx()); 
				profileStep.handle(sandboxedCtx.getAuthnContext().getAuthnInput());
				sandboxStep.enableNext();
				wizard.next();
				wizard.getBackButton().setEnabled(false);				
			}

			@Override
			public void onCompleteAuthnResult(AuthenticatedEntity authenticatedEntity)
			{
			}
		}, wizard, UI.getCurrent(), false);
		
		return wizard;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("Wizard.wizardCaption");
	}
}
