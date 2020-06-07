/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webadmin;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.msg.MessageAreaProvider;

@Component
public class WebAdminCommonMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "webadmin-common";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "WebAdminCommonMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
