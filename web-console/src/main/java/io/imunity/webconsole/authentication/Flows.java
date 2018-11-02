/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webconsole.idprovider.SAML;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Lists all flows
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class Flows extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Flows";

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();

		Label title = new Label();
		title.setValue("Flows");
		main.addComponent(title);

		Button link2 = new Button();
		link2.setCaption("Go to SAML");
		link2.addClickListener(e -> {
			getUI().getNavigator().navigateTo(SAML.class.getSimpleName());
		});

		main.addComponent(link2);
		setCompositionRoot(main);

	}

	@Component
	public static class FlowsNavigationInfoProvider implements WebConsoleNavigationInfoProvider
	{

		private UnityMessageSource msg;
		private AuthenticationNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public FlowsNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<Flows> factory)
		{
			this.msg = msg;
			this.parent = parent;
			this.factory = factory;

		}

		@Override
		public NavigationInfo getNavigationInfo()
		{

			return new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withDisplayNameProvider(e -> msg.getMessage(
							"WebConsoleMenu.authentication.flows"))
					.build();
		}
	}
}
