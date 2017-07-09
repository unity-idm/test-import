/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jvnet.libpam.UnixUser;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Shared helpers - allows to convert PAM library's objects to version meaningful for Unity platform. 
 * @author K. Benedyczak
 */
public class LibPAMUtils
{
	public static RemotelyAuthenticatedInput unixUser2RAI(UnixUser unixUser, String idp)
	{
		RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idp);
		List<RemoteAttribute> attributes = new ArrayList<>();
		attributes.add(new RemoteAttribute("gid", unixUser.getGID()));
		attributes.add(new RemoteAttribute("uid", unixUser.getUID()));
		if (unixUser.getDir() != null)
			attributes.add(new RemoteAttribute("home", unixUser.getDir()));
		if (unixUser.getGecos() != null)
			attributes.add(new RemoteAttribute("gecos", unixUser.getGecos()));
		if (unixUser.getShell() != null)
			attributes.add(new RemoteAttribute("shell", unixUser.getShell()));
		ret.setAttributes(attributes);
		
		ret.setGroups(unixUser.getGroups().stream()
				.map(RemoteGroupMembership::new)
				.collect(Collectors.toList()));
		ret.setIdentities(Lists.newArrayList(new RemoteIdentity(unixUser.getUserName(),
				UsernameIdentity.ID)));
		return ret;
	}
}
