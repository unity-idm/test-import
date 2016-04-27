/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.mapper;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.NamedCRUDMapper;
import pl.edu.icm.unity.store.rdbms.model.GroupBean;


/**
 * Access to the Groups.xml operations.
 * @author K. Benedyczak
 */
public interface GroupsMapper extends NamedCRUDMapper<GroupBean>
{
	List<GroupBean> getSubgroups(String parentPath);
}
