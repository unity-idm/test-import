/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.TextField;

public class ReadOnlyField extends TextField
{
	public ReadOnlyField(String value, float width, Unit widthUnit)
	{
		super();
		setValue(value);
		setWidth(width, widthUnit);
		setReadOnly(true);
	}
	
	public ReadOnlyField(String value)
	{
		super();
		setValue(value);
		setReadOnly(true);
	}
}