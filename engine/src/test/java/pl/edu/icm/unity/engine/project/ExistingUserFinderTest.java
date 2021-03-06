/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

public class ExistingUserFinderTest
{
	private static final EmailIdentity emailIdType = new EmailIdentity();
	private BulkGroupQueryService bulkService = mock(BulkGroupQueryService.class);
	private ProjectAttributeHelper attrHelper = mock(ProjectAttributeHelper.class);
	
	@Test
	public void shouldFindByIdentityCaseInsensitive() throws EngineException
	{
		EntityInGroupData entityData = new EntityInGroupData(
				createEmailEntity("addr1@EXample.com", 13), 
				null, 
				null, 
				null, 
				null, 
				null);
		when(bulkService.getMembershipInfo(any())).thenReturn(ImmutableMap.of(13l, entityData));
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper);
		
		Long entityIdByContactAddress = userFinder.getEntityIdByContactAddress("Addr1@examplE.com");
		
		assertThat(entityIdByContactAddress).isEqualTo(13);
	}

	@Test
	public void shouldFindByAttributeCaseInsensitive() throws EngineException
	{
		AttributeExt emailAttr = new AttributeExt(VerifiableEmailAttribute.of("email", "/", "addr1@EXample.com"), true);
		EntityInGroupData entityData = new EntityInGroupData(
				createEmailEntity("other@example.com", 13), 
				null, 
				null, 
				ImmutableMap.of("email", emailAttr), 
				null, 
				null);
		when(bulkService.getMembershipInfo(any())).thenReturn(ImmutableMap.of(13l, entityData));
		when(attrHelper.searchVerifiableAttributeValueByMeta(eq(ContactEmailMetadataProvider.NAME), any()))
			.thenReturn(VerifiableEmail.fromJsonString(emailAttr.getValues().get(0)));
		ExistingUserFinder userFinder = new ExistingUserFinder(bulkService, attrHelper);
		
		Long entityIdByContactAddress = userFinder.getEntityIdByContactAddress("Addr1@examplE.com");
		
		assertThat(entityIdByContactAddress).isEqualTo(13);		
	}

	private Entity createEmailEntity(String email, long entityId) throws IllegalIdentityValueException
	{
		IdentityParam idParam = emailIdType.convertFromString(email, "ridp", null);
		Identity identity = new Identity(idParam, entityId, emailIdType.getComparableValue(idParam.getValue(), "realm", null));
		return new Entity(Lists.newArrayList(identity), new EntityInformation(13), null);
	}
}
