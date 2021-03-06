/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation;

import java.util.List;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Parses external data into types usable in Unity API.
 */
public interface ExternalDataParser
{
	Attribute parseAsAttribute(String unityAttributeName, String group, List<?> externalValues)
			throws IllegalAttributeValueException;

	Attribute parseAsAttribute(AttributeType unityAttribute,
			String group,
			List<?> externalValues,
			String idp,
			String profile) throws IllegalAttributeValueException;

	<T> Attribute parseAsConfirmedAttribute(AttributeType unityAttribute,
			String group,
			List<?> externalValues,
			String idp,
			String profile) throws IllegalAttributeValueException;

	IdentityParam parseAsIdentity(String identityType, Object externalValue) throws IllegalIdentityValueException;

	IdentityParam parseAsIdentity(IdentityTypeDefinition identityType, Object externalValue, String idp, String profile)
			throws IllegalIdentityValueException;

	IdentityParam parseAsConfirmedIdentity(IdentityTypeDefinition identityType,
			Object externalValue,
			String idp,
			String profile) throws IllegalIdentityValueException;
}
