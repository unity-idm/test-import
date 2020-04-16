/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AuditEventManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

public class AuditEventListenerTest extends DBIntegrationTestBase
{
	private static final Duration DEFAULT_WAIT_TIME = new Duration(20, TimeUnit.SECONDS);
	@Autowired
	private AuditEventManagement auditManager;

	@Autowired
	private AuditEventListener auditListener;

	@Autowired
	private AttributeTypeManagement attributeTypeMan;

	private AttributeType typeWithEntityName;

	@Before
	public void setup() throws Exception
	{
		typeWithEntityName = getType();
		auditManager.enableAuditEvents();
	}

	@After
	public void cleanup()
	{
		auditManager.disableAuditEvents();
	}

	@After
	public void after() 
	{
		// make sure entityNameAttribute is to null after test
		auditListener.entityNameAttribute = null;
	}

	@Test
	public void shouldInitializeEntityNameAttributeToNullAfterDbReset() 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
	}

	@Test
	public void shouldSetEntityNameAttributeWhenAttributeIsAdded() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);

		//when
		attributeTypeMan.addAttributeType(typeWithEntityName);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> typeWithEntityName.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	@Test
	public void shouldUnsetEntityNameAttributeWhenAttributeIsRemoved() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		initializeAttributeTypeWithEntityName();

		//when
		attributeTypeMan.removeAttributeType(typeWithEntityName.getName(), false);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> (auditListener.entityNameAttribute == null));
	}

	@Test
	public void shouldUnsetEntityNameAttributeWhenMetadataIsRemoved() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		initializeAttributeTypeWithEntityName();

		//when
		typeWithEntityName.setMetadata(Collections.emptyMap());
		attributeTypeMan.updateAttributeType(typeWithEntityName);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> (auditListener.entityNameAttribute == null));
	}

	@Test
	public void shouldSetEntityNameAttributeWhenMetadataIsAdded() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		AttributeType type = getType();
		type.setMetadata(Collections.emptyMap());
		attributeTypeMan.addAttributeType(type);

		//when
		assertNull(auditListener.entityNameAttribute);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		type.setMetadata(meta);
		attributeTypeMan.updateAttributeType(type);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> type.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	@Test
	public void shouldSetEntityNameAttributeWhenMetadataRemovedAndAddedToOtherAttribute() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		initializeAttributeTypeWithEntityName();
		AttributeType type = getType();
		type.setName("newName");
		type.setMetadata(Collections.emptyMap());
		attributeTypeMan.addAttributeType(type);
		assertEquals(auditListener.entityNameAttribute, typeWithEntityName.getName());

		//when
		typeWithEntityName.setMetadata(Collections.emptyMap());
		attributeTypeMan.updateAttributeType(typeWithEntityName);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		type.setMetadata(meta);
		attributeTypeMan.updateAttributeType(type);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> type.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	private void initializeAttributeTypeWithEntityName() throws EngineException 
	{
		attributeTypeMan.addAttributeType(typeWithEntityName);
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> typeWithEntityName.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	private AttributeType getType() 
	{
		AttributeType type = new AttributeType("theName", "string");
		type.setDescription(new I18nString("desc"));
		type.setDisplayedName(new I18nString("Displayed name"));
		type.setUniqueValues(true);
		type.setMaxElements(1);
		type.setMinElements(1);
		type.setSelfModificable(true);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		type.setMetadata(meta);
		return type;
	}
}