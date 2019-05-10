/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.file.ImageUtils;

/**
 * Retrieves the authenticated user from the TLS. The login happens on the HTTP connection level 
 * and so the component is not interactive.
 * 
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("WebTLSRetrieval")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TLSRetrieval extends AbstractCredentialRetrieval<CertificateExchange> implements VaadinAuthentication
{
	public static final String NAME = "web-certificate";
	public static final String DESC = "WebTLSRetrievalFactory.desc";
	
	private UnityMessageSource msg;
	private URIAccessService uriAccessService;
	private I18nString name;
	private String logoURL;
	private String registrationFormForUnknown;
	private boolean enableAssociation;
	private String configuration;
	
	@Autowired
	public TLSRetrieval(UnityMessageSource msg, URIAccessService uriAccessService)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.uriAccessService = uriAccessService;
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		return configuration;
	}

	@Override
	public void setSerializedConfiguration(String configuration)
	{
		this.configuration = configuration;
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(configuration));
			TLSRetrievalProperties config = new TLSRetrievalProperties(properties);
			name = config.getLocalizedString(msg, TLSRetrievalProperties.NAME);
			if (name.isEmpty())
				name = new I18nString("WebTLSRetrieval.title", msg);
			logoURL = config.getValue(TLSRetrievalProperties.LOGO_URL);
			if (logoURL != null && !logoURL.isEmpty())
			{
				URIHelper.parseURI(logoURL);
			}
			registrationFormForUnknown = config.getValue(
					TLSRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN);
			enableAssociation = config.getBooleanValue(TLSRetrievalProperties.ENABLE_ASSOCIATION);
			
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based TLS retrieval can not be parsed", e);
		}
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context)
	{
		return Collections.<VaadinAuthenticationUI>singleton(new TLSRetrievalUI(uriAccessService));
	}

	@Override
	public boolean supportsGrid()
	{
		return false; //TODO this component can support grid
	}
	
	public static X509Certificate[] getTLSCertificate()
	{
		HttpServletRequest request = VaadinServletService.getCurrentServletRequest();
		if (request == null)
			return null;
		return (X509Certificate[]) request.getAttribute(
				"javax.servlet.request.X509Certificate");
	} 
	
	private class TLSRetrievalUI implements VaadinAuthenticationUI
	{
		private URIAccessService uriAccessService;
		
		private Component component = new TLSAuthnComponent();
		private AuthenticationCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;
		
		public TLSRetrievalUI(URIAccessService uriAccessService)
		{
			this.uriAccessService = uriAccessService;
		}

		@Override
		public Component getComponent()
		{
			return component;
		}

		@Override
		public void setAuthenticationCallback(AuthenticationCallback callback)
		{
			this.callback = callback;
		}

		private AuthenticationResult getAuthenticationResult()
		{
			X509Certificate[] clientCert = getTLSCertificate();

			if (clientCert == null)
				return new AuthenticationResult(Status.notApplicable, null);

			AuthenticationResult authenticationResult = credentialExchange.checkCertificate(clientCert,
					sandboxCallback);
			if (registrationFormForUnknown != null)
				authenticationResult.setFormForUnknownPrincipal(registrationFormForUnknown);
			authenticationResult.setEnableAssociation(enableAssociation);
			return authenticationResult;
		}

		@Override
		public String getLabel()
		{
			return name.getValue(msg);
		}

		@Override
		public Resource getImage()
		{
			if (logoURL == null)
				return null;
			if ("".equals(logoURL))
				return Images.certificate.getResource();
			else
			{
				
				return ImageUtils.getConfiguredImageResourceFromUriSave(logoURL, uriAccessService);
			}

		}

		private class TLSAuthnComponent extends VerticalLayout
		{
			private Button authenticateButton;

			public TLSAuthnComponent()
			{
				setMargin(false);
				setSpacing(true);
				X509Certificate[] clientCert = getTLSCertificate();
				String info = clientCert == null ? "" : msg.getMessage("WebTLSRetrieval.certInfo", 
						X500NameUtils.getReadableForm(clientCert[0].getSubjectX500Principal()));
				authenticateButton = new Button(msg.getMessage("WebTLSRetrieval.signInButton"));
				authenticateButton.addClickListener(event -> triggerAuthentication());
				authenticateButton.setIcon(getImage());
				authenticateButton.addStyleName(Styles.signInButton.toString());
				authenticateButton.addStyleName("u-x509SignInButton");
				authenticateButton.setWidth(100, Unit.PERCENTAGE);
				authenticateButton.setDescription(info);
				addComponent(authenticateButton);
			}
		}
		
		private void triggerAuthentication()
		{
			callback.onStartedAuthentication(AuthenticationStyle.IMMEDIATE);
			AuthenticationResult authenticationResult = getAuthenticationResult();
			if (authenticationResult.getStatus() == Status.success)
				component.setEnabled(false);
			
			callback.onCompletedAuthentication(authenticationResult);
		}
		
		@Override
		public boolean isAvailable()
		{
			return getTLSCertificate() != null;
		}
		
		@Override
		public void clear()
		{
			//nop
		}

		@Override
		public void refresh(VaadinRequest request) 
		{
			//nop
		}

		@Override
		public void setSandboxAuthnCallback(SandboxAuthnResultCallback callback) 
		{
			sandboxCallback = callback;
		}

		/**
		 * Simple: there is only one authN option in this authenticator so we can return any constant id. 
		 */
		@Override
		public String getId()
		{
			return "certificate";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
		}
		

		@Override
		public Set<String> getTags()
		{
			return Collections.emptySet();
		}
	}
	
	
	@org.springframework.stereotype.Component("WebTLSRetrievalFactory")
	public static class Factory extends AbstractCredentialRetrievalFactory<TLSRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<TLSRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, CertificateExchange.ID);
		}
	}
}


