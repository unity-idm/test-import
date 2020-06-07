/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.msg.MessageAreaProvider;

@Component
public class WebConsoleUtilsMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "webconsoleutils";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "WebConsoleUtilsMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
