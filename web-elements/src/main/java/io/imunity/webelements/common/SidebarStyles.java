/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webelements.common;

/**
 * General purpose styles defined for Sidebar layout components
 * @author P.Piernik
 *
 */
public enum SidebarStyles
{
	leftMenu("u-leftMenu"),
	topRightMenu("u-topRightMenu"),
	headerBar("u-headerBar"),
	sidebar("u-sidebar"),
	contentBox("u-contentBox"),
	rootContent("u-rootContent"),
	menuButton("u-menuButton"),
	menuButtonClickable("u-clickable"),
	menuButtonActive("u-active"),
	menuLabel("u-menuLabel"),
	menuCombo("u-menuCombo"),
	subMenu("u-subMenu"),
	subMenuOpen("u-open"),
	tooltip("u-toolTip");

	private String value;

	private SidebarStyles(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
