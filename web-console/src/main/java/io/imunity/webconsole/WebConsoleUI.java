/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.PushStateNavigation;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.authentication.Flows;
import io.imunity.webconsole.authentication.Routes;
import io.imunity.webconsole.authentication.realms.NewRealm;
import io.imunity.webconsole.authentication.realms.Realms;
import io.imunity.webconsole.idprovider.OAuth;
import io.imunity.webconsole.idprovider.SAML;
import io.imunity.webconsole.layout.LeftMenu;
import io.imunity.webconsole.layout.TopMenu;
import io.imunity.webconsole.layout.WebConsoleLayout;
import io.imunity.webconsole.leftmenu.components.MenuButton;
import io.imunity.webconsole.leftmenu.components.SubMenu;
import io.imunity.webconsole.leftmenu.components.MenuLabelClickable;
import io.imunity.webconsole.other.OtherServices;
import io.imunity.webconsole.topmenu.components.TopMenuTextField;
import io.imunity.webconsole.userprofile.UserProfile;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * The main entry point of the web console UI.
 * 
 * @author P.Piernik
 *
 */
@PushStateNavigation
@Component("WebConsoleUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class WebConsoleUI extends UnityEndpointUIBase implements UnityWebUI
{
	private StandardWebAuthenticationProcessor authnProcessor;

	private WebConsoleLayout webConsoleLayout = null;

	@Autowired
	public WebConsoleUI(UnityMessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher,
			StandardWebAuthenticationProcessor authnProcessor)
	{
		super(msg, enquiryDialogLauncher);
		this.authnProcessor = authnProcessor;
	}

	private void setDefaultPage()
	{
		UI.getCurrent().getNavigator().setErrorView(Dashboard.class);
		
	}

	private void buildTopOnlyMenu()
	{
		TopMenu topMenu = webConsoleLayout.getTopMenu();
		topMenu.add(TopMenuTextField.get(VaadinIcons.SEARCH, "Search ..."));
		topMenu.add(MenuButton.get().withIcon(VaadinIcons.HOME)
				.withDescription(msg.getMessage("WebConsoleUIMenu.dashboard"))
				.withNavigateTo(Dashboard.class));

		topMenu.add(MenuButton.get().withIcon(Images.exit.getResource())
				.withDescription(msg.getMessage("WebConsoleUIMenu.logout"))
				.withClickListener(e -> logout()));

	}

	private void logout()
	{
		authnProcessor.logout();
	}

	private void buildLeftMenu()
	{

		LeftMenu leftMenu = webConsoleLayout.getLeftMenu();
		MenuLabelClickable label = MenuLabelClickable.get()
				.withIcon(Images.logoSmall.getResource());

		label.addLayoutClickListener(e -> {
			webConsoleLayout.getLeftMenu().toggleSize();
		});
		leftMenu.add(label);

		leftMenu.setHeight(100, Unit.PERCENTAGE);

		MenuButton dashboard = leftMenu.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.dashboard"))
				.withIcon(VaadinIcons.HOME).withNavigateTo(Dashboard.class));

		webConsoleLayout.getBreadCrumbs().setRoot(dashboard);

		SubMenu idprovider = leftMenu.add(SubMenu.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.idprovider"))
				.withIcon(Images.ok.getResource()));

		idprovider.add(MenuButton.get().withCaption(msg.getMessage("WebConsoleUIMenu.oauth"))
				.withIcon(Images.ok.getResource()).withNavigateTo(OAuth.class));

		idprovider.add(MenuButton.get().withCaption(msg.getMessage("WebConsoleUIMenu.saml"))
				.withIcon(Images.ok.getResource()).withNavigateTo(SAML.class));

		leftMenu.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.userProfile"))
				.withIcon(Images.ok.getResource())
				.withNavigateTo(UserProfile.class));

		leftMenu.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.otherServices"))
				.withIcon(Images.ok.getResource())
				.withNavigateTo(OtherServices.class));

		SubMenu authentication = leftMenu.add(SubMenu.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.authentication"))
				.withIcon(Images.ok.getResource()));

		MenuButton realms = MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.realms"))
				.withIcon(Images.ok.getResource()).withNavigateTo(Realms.class);

		MenuButton newRealm = MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.newRealm"))
				.withNavigateTo(NewRealm.class);

		realms.add(newRealm);

		authentication.add(realms);

		authentication.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.flows"))
				.withIcon(Images.ok.getResource()).withNavigateTo(Flows.class));

		authentication.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.routes"))
				.withIcon(Images.ok.getResource()).withNavigateTo(Routes.class));

	}

	@Override
	protected void enter(VaadinRequest request)
	{
		webConsoleLayout = WebConsoleLayout.get().withNaviContent(new VerticalLayout()).build();
		buildTopOnlyMenu();
		buildLeftMenu();
		setDefaultPage();
		setContent(webConsoleLayout);
	}
}
