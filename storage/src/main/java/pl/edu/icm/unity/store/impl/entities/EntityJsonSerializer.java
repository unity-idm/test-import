/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.EntityInformation;

/**
 * Serializes {@link StoredEntity} to/from DB form.
 * @author K. Benedyczak
 */
@Component
public class EntityJsonSerializer implements RDBMSObjectSerializer<EntityInformation, BaseBean>, 
		JsonSerializerForKryo<EntityInformation>
{
	@Override
	public BaseBean toDB(EntityInformation object)
	{
		BaseBean ret = new BaseBean(null, JsonUtil.serialize2Bytes(object.toJsonBase()));
		ret.setId(object.getId());
		return ret;
	}

	@Override
	public EntityInformation fromDB(BaseBean bean)
	{
		EntityInformation ret = new EntityInformation(bean.getId());
		ret.fromJsonBase(JsonUtil.parse(bean.getContents()));
		return ret;
	}
	
	@Override
	public EntityInformation fromJson(ObjectNode src)
	{
		return new EntityInformation(src);
	}

	@Override
	public ObjectNode toJson(EntityInformation src)
	{
		return src.toJson();
	}
	
	@Override
	public Class<EntityInformation> getClazz()
	{
		return EntityInformation.class;
	}
}
