/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.LinkableImage;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.ext.AttributeHandlerHelper;

class PublicLinkableImageAttributeHandler implements WebAttributeHandler
{
	private final UnityMessageSource msg;
	private final PublicLinkableImageSyntax syntax;

	PublicLinkableImageAttributeHandler(UnityMessageSource msg, PublicLinkableImageSyntax syntax)
	{
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public String getValueAsString(String value)
	{
		return PublicLinkableImageSyntax.ID;
	}

	@Override
	public Component getRepresentation(String valueRaw, AttributeViewerContext context)
	{
		LinkableImage value = syntax.convertFromString(valueRaw);
		if (value.getUnityImage() != null)
			return new ImageRepresentationComponent(value.getUnityImage(), context);
		if (value.getUrl() != null)
			return AttributeHandlerHelper.getRepresentation(value.getUrl().toExternalForm(), context);
		return AttributeHandlerHelper.getRepresentation("", context);
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new PublicLinkableImageValueEditor(initialValue, label, msg, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new CompactFormLayout(UnityImageValueComponent.getHints(syntax.getConfig(), msg));
	}
}
