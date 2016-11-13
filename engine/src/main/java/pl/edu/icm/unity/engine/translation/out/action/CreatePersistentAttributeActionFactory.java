/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Creates new outgoing attributes.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreatePersistentAttributeActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "createPersistentAttribute";
	private AttributeTypeDAO attrsMan;
	private AttributeValueConverter attrValueConverter;
	
	@Autowired
	public CreatePersistentAttributeActionFactory(AttributeTypeDAO attrsMan, 
			AttributeValueConverter attrValueConverter)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"attributeName",
						"TranslationAction.createPersistentAttribute.paramDesc.attributeName",
						Type.UNITY_ATTRIBUTE),
				new ActionParameterDefinition(
						"expression",
						"TranslationAction.createPersistentAttribute.paramDesc.expression",
						Type.EXPRESSION),
				new ActionParameterDefinition(
						"group",
						"TranslationAction.createPersistentAttribute.paramDesc.group",
						Type.UNITY_GROUP)
		});
		this.attrsMan = attrsMan;
		this.attrValueConverter = attrValueConverter;
	}

	@Override
	public CreatePersistentAttributeAction getInstance(String... parameters)
	{
		return new CreatePersistentAttributeAction(parameters, getActionType(), attrsMan, attrValueConverter);
	}
	
	public static class CreatePersistentAttributeAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, 
				CreatePersistentAttributeAction.class);
		private String attrNameString;
		private AttributeType attributeType;
		private Serializable valuesExpression;
		private String group;
		private AttributeValueConverter attrValueConverter;
		

		public CreatePersistentAttributeAction(String[] params, TranslationActionType desc, 
				AttributeTypeDAO attrsMan, AttributeValueConverter attrValueConverter)
		{
			super(desc, params);
			this.attrValueConverter = attrValueConverter;
			setParameters(params, attrsMan);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Object value = MVEL.executeExpression(valuesExpression, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Attribute value evaluated to null, skipping");
				return;
			}
			for (Attribute existing: result.getAttributes())
			{
				if (existing.getName().equals(attrNameString))
				{
					log.trace("Attribute already exists, skipping");
					return;
				}
			}
			List<?> aValues = value instanceof List ? (List<?>)value : Collections.singletonList(value);
			
			List<String> typedValues;
			try
			{
				typedValues = attrValueConverter.externalValuesToInternal(
						attributeType.getName(), aValues);
			} catch (IllegalAttributeValueException e)
			{
				log.debug("Can't convert attribute values returned by the action's expression "
						+ "to the type of attribute " + attrNameString + " , skipping it", e);
				return;
			}
			//for output profile we can't confirm - not yet implemented and rather not needed.
			for (Object val: typedValues)
			{
				if (val instanceof VerifiableElement)
				{
					((VerifiableElement) val).setConfirmationInfo(new ConfirmationInfo(true));
				}
			}

			Attribute newAttr = new Attribute(attrNameString, attributeType.getValueSyntax(), group, 
					typedValues, null, currentProfile);
			result.getAttributes().add(newAttr);
			result.getAttributesToPersist().add(newAttr);
			log.debug("Created a new persisted attribute: " + newAttr);
		}

		private void setParameters(String[] parameters, AttributeTypeDAO attrsMan)
		{
			if (parameters.length != 3)
				throw new IllegalArgumentException("Action requires exactly 3 parameters");
			attrNameString = parameters[0];
			valuesExpression = MVEL.compileExpression(parameters[1]);
			group = parameters[2];

			attributeType = attrsMan.get(attrNameString);
			if (attributeType == null)
				throw new IllegalArgumentException("The attribute type " + parameters[0] + 
						" is not a valid Unity attribute type and therefore can not "
						+ "be persisted");
			if (attributeType.isInstanceImmutable())
				throw new IllegalArgumentException("The attribute type " + parameters[0] + 
						" is managed internally only so it can not be persisted");
		}

	}
}
