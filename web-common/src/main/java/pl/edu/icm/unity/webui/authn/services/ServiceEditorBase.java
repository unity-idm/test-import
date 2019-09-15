/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Base for all service editor. Editor based on two tabs - general and
 * authentication.
 * 
 * @author P.Piernik
 *
 */
public abstract class ServiceEditorBase extends CustomComponent implements ServiceEditorComponent
{
	protected UnityMessageSource msg;

	private Map<ServiceEditorComponent.ServiceEditorTab, Tab> defTabs;
	private TabSheet tabs;

	public ServiceEditorBase(UnityMessageSource msg)
	{
		this.msg = msg;

		defTabs = new HashMap<>();
		tabs = new TabSheet();

		tabs.addSelectedTabChangeListener(e -> {
			setErrorInTabs();
		});
		setCompositionRoot(tabs);

	}

	protected void setErrorInTabs()
	{

		for (Tab tab : defTabs.values())
		{
			tab.setComponentError(null);

			if (assertErrorComponent(tab.getComponent()))
			{
				tab.setComponentError(new UserError(msg.getMessage("error")));
			}

		}
	}

	boolean assertErrorComponent(Component component)
	{

		if (component instanceof AbstractComponent)
		{
			AbstractComponent ac = (AbstractComponent) component;
			if (ac.getComponentError() != null)
				return true;
		}

		if (component instanceof HasComponents)
		{
			HasComponents ac = (HasComponents) component;
			Iterator<Component> it = ac.iterator();
			while (it.hasNext())
			{
				if (assertErrorComponent(it.next()))
				{
					return true;
				}
			}
		}

		return false;
	}

	protected void registerTab(EditorTab editorTab)
	{
		
		Tab tab = tabs.addTab(editorTab.getComponent());
		tab.setCaption(editorTab.getCaption());
		tab.setIcon(editorTab.getIcon());
		defTabs.put(editorTab.getType(), tab);
	}

	@Override
	public void setActiveTab(ServiceEditorTab tab)
	{
		tabs.setSelectedTab(defTabs.get(tab));
	}
	
	public interface EditorTab
	{
		Resource getIcon();
		ServiceEditorTab getType();
		CustomComponent getComponent();
		String getCaption();	
	}
	

}