/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.JsonSerializer;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Serializes {@link Attribute} to/from JSON.
 * @author K. Benedyczak
 */
@Component
public class AttributeJsonSerializer implements JsonSerializer<Attribute<?>>
{
	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private AttributeTypeDAO atDAO;
	
	@SuppressWarnings("unchecked")
	@Override
	public Attribute<?> fromJson(ObjectNode main)
	{
		if (main == null)
			return null;
		String name = main.get("name").asText();
		AttributeType type = atDAO.get(name);
		@SuppressWarnings("rawtypes")
		Attribute ret = new Attribute();
		ret.setAttributeSyntax(type.getValueType());
		ret.setGroupPath(main.get("groupPath").asText());
		ret.setName(name);

		fromJsonBase(main, ret);
		return ret;
	}

	@Override
	public ObjectNode toJson(Attribute<?> src)
	{
		ObjectNode root = toJsonBase(src);
		root.put("name", src.getName());
		root.put("groupPath", src.getGroupPath());
		return root;
	}

	protected <T> ObjectNode toJsonBase(Attribute<T> src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("visibility", src.getVisibility().name());
		if (src.getRemoteIdp() != null)
			root.put("remoteIdp", src.getRemoteIdp());
		if (src.getTranslationProfile() != null)
			root.put("translationProfile", src.getTranslationProfile());
		ArrayNode values = root.putArray("values");
		AttributeValueSyntax<T> syntax = src.getAttributeSyntax();
		for (T value: src.getValues())
			values.add(syntax.serialize(value));
		return root;
	}
	
	protected <T> void fromJsonBase(ObjectNode main, Attribute<T> target)
	{
		target.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		
		if (main.has("translationProfile"))
			target.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("remoteIdp"))
			target.setRemoteIdp(main.get("remoteIdp").asText());
		
		ArrayNode values = main.withArray("values");
		List<T> pValues = new ArrayList<T>(values.size());
		Iterator<JsonNode> it = values.iterator();
		AttributeValueSyntax<T> syntax = target.getAttributeSyntax();
		try
		{
			while(it.hasNext())
				pValues.add(syntax.deserialize(it.next().binaryValue()));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		target.setValues(pValues);
	}
}
