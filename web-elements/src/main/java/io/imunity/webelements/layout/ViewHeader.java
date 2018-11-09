/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.layout;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Component for displaying the unity view header
 * 
 * @author P.Piernik
 *
 */
public class ViewHeader extends CustomComponent implements ViewChangeListener
{
	private HorizontalLayout main;

	public ViewHeader()
	{
		main = new HorizontalLayout();
		main.setStyleName(Styles.viewHeader.toString());
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);
	}

	@Override
	public boolean beforeViewChange(ViewChangeEvent event)
	{
		return true;
	}

	@Override
	public void afterViewChange(ViewChangeEvent event)
	{
		main.removeAllComponents();
		View uView = event.getNewView();
		if (uView instanceof UnityView)
		{
			main.addComponent(((UnityView) uView).getViewHeader());
		}
	}

}
