/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.ws.console;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.saml.idp.console.SAMLEditorClientsTab;
import pl.edu.icm.unity.saml.idp.console.SAMLEditorGeneralTab;
import pl.edu.icm.unity.saml.idp.console.SAMLUsersEditorTab;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.idp.IdpEditorUsersTab;
import pl.edu.icm.unity.webui.console.services.idp.IdpUser;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

/**
 * SAML SOAP Service editor
 * 
 * @author P.Piernik
 *
 */
public class SAMLSoapServiceEditor implements ServiceEditor
{
	private MessageSource msg;
	private EndpointTypeDescription type;
	private PKIManagement pkiMan;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private SAMLSoapServiceEditorComponent editor;
	private List<String> allAttributes;
	private List<Group> allGroups;
	private List<IdpUser> allUsers;
	private Set<String> credentials;
	private Set<String> truststores;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private String serverPrefix;
	private Set<String> serverContextPaths;
	private Collection<IdentityType> idTypes;
	private SubViewSwitcher subViewSwitcher;
	private OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private List<String> usedPaths;
	private ImageAccessService imageAccessService;

	public SAMLSoapServiceEditor(MessageSource msg, EndpointTypeDescription type, PKIManagement pkiMan,
			SubViewSwitcher subViewSwitcher,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory, String serverPrefix,
			Set<String> serverContextPaths,
			URIAccessService uriAccessService, ImageAccessService imageAccessService,
			FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allAttributes, List<Group> allGroups, List<IdpUser> allUsers,
			Set<String> credentials, Set<String> truststores, Collection<IdentityType> idTypes,
			List<String> usedPaths)
	{
		this.msg = msg;
		this.type = type;
		this.imageAccessService = imageAccessService;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.allAttributes = allAttributes;
		this.allGroups = allGroups;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.credentials = credentials;
		this.serverPrefix = serverPrefix;
		this.serverContextPaths = serverContextPaths;
		this.idTypes = idTypes;
		this.subViewSwitcher = subViewSwitcher;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.usedPaths = usedPaths;
		this.allUsers = allUsers;
		this.truststores = truststores;
		this.pkiMan = pkiMan;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		
		SAMLEditorGeneralTab samlEditorGeneralTab = new SAMLEditorGeneralTab(msg, serverPrefix, serverContextPaths, 
				serverConfig, subViewSwitcher,
				outputTranslationProfileFieldFactory,
				usedPaths, credentials, truststores, idTypes);
		
		SAMLEditorClientsTab clientsTab = new SAMLEditorClientsTab(msg, pkiMan, serverConfig, uriAccessService,
				fileStorageService, subViewSwitcher);
		
		IdpEditorUsersTab usersTab = new SAMLUsersEditorTab(msg, allGroups, allUsers,
				allAttributes);
		
		AuthenticationTab authTab = new AuthenticationTab(msg, flows, authenticators, allRealms, type.getSupportedBinding());
		
		editor = new SAMLSoapServiceEditorComponent(msg, samlEditorGeneralTab, clientsTab, usersTab, authTab,
				type, pkiMan, uriAccessService, imageAccessService, fileStorageService, endpoint, allGroups);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
