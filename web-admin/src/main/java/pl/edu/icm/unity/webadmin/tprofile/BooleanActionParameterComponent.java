/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.ui.CheckBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Action parameter for boolean 
 * @author P.Piernik
 *
 */
public class BooleanActionParameterComponent extends CheckBox implements ActionParameterComponent
{

	public BooleanActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(desc.getName());
		setDescription(msg.getMessage(desc.getDescriptionKey()));
	}
	
	@Override
	public String getActionValue()
	{
		return getValue().toString();
	}

	@Override
	public void setActionValue(String value)
	{
		setValue(Boolean.valueOf(value));
		
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		addValueChangeListener((e) -> { callback.run(); });		
	}
}
