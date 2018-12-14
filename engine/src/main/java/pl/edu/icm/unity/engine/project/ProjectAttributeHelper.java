/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * A collection of methods useful in projects management impl
 * @author P.Piernik
 *
 */
@Component
public class ProjectAttributeHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, ProjectAttributeHelper.class);

	private AttributesManagement attrMan;
	private AttributesHelper attrHelper;
	private AttributeTypeHelper atHelper;

	@Autowired
	public ProjectAttributeHelper(@Qualifier("insecure")AttributesManagement attrMan, AttributesHelper attrHelper,
			AttributeTypeHelper atHelper)
	{
		this.attrMan = attrMan;
		this.attrHelper = attrHelper;
		this.atHelper = atHelper;
	}

	public Optional<Attribute> getAttribute(long entityId, String path, String attribute) throws EngineException
	{
		Collection<AttributeExt> attributes = attrMan.getAttributes(new EntityParam(entityId), path, attribute);

		if (!attributes.isEmpty())
		{
			return Optional.ofNullable(attributes.iterator().next());
		}
		return Optional.empty();
	}

	public Optional<String> getAttributeValue(long entityId, String path, String attribute) throws EngineException
	{

		Optional<Attribute> attr = getAttribute(entityId, path, attribute);
		if (attr.isPresent())
		{
			if (!attr.get().getValues().isEmpty())
			{
				return Optional.ofNullable(attr.get().getValues().iterator().next());
			}
		}
		return Optional.empty();
	}

	public String searchAttributeValueByMeta(String metadata, Collection<Attribute> list) throws EngineException
	{
		AttributeType attrType = attrHelper.getAttributeTypeWithSingeltonMetadata(metadata);
		if (attrType == null)
			return null;

		String attrName = attrType.getName();

		for (Attribute attr : list)
		{
			if (attr.getName().equals(attrName) && attr.getValues() != null && !attr.getValues().isEmpty())
			{

				return getVerifiableAttributeValue(getAttributeSyntaxSafe(attrName),
						attr.getValues().get(0));
			}
		}
		return null;
	}

	private String getVerifiableAttributeValue(AttributeValueSyntax<?> attributeSyntax, String value)
	{

		if (attributeSyntax != null && attributeSyntax.isEmailVerifiable() && value != null)
		{
			VerifiableEmail email = (VerifiableEmail) attributeSyntax.convertFromString(value);
			return email.getValue();
		}
		return value;
	}

	private AttributeValueSyntax<?> getAttributeSyntaxSafe(String attributeName)
	{
		try
		{
			return atHelper.getUnconfiguredSyntaxForAttributeName(attributeName);
		} catch (Exception e)
		{
			// ok
			log.debug("Can not get attribute syntax for attribute " + attributeName);
			return null;
		}
	}

	public String getAttributeFromMeta(long entityId, String path, String metadata) throws EngineException
	{
		AttributeType attrType = attrHelper.getAttributeTypeWithSingeltonMetadata(metadata);
		if (attrType == null)
			return null;

		Optional<String> value = getAttributeValue(entityId, path, attrType.getName());

		if (!value.isPresent())
			return null;

		AttributeValueSyntax<?> syntax = getAttributeSyntaxSafe(attrType.getName());
		return getVerifiableAttributeValue(syntax, value.get());
	}

}
