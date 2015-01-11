/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.IdentityFromRegState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registrations.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Identity from registration confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class IdentityFromRegistrationRequestFacility extends
		AttributeFromRegistrationRequestFacility implements ConfirmationFacility
{

	private IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	public IdentityFromRegistrationRequestFacility(DBSessionManager db,
			RegistrationRequestDB requestDB, RegistrationFormDB formsDB,
			InternalRegistrationManagment internalRegistrationManagment,
			IdentityTypesRegistry identityTypesRegistry)
	{
		super(db, requestDB, formsDB, internalRegistrationManagment);
		this.identityTypesRegistry = identityTypesRegistry;
	}

	@Override
	public String getName()
	{
		return IdentityFromRegState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity from registration request";
	}

	private IdentityFromRegState getState(String state)
	{
		IdentityFromRegState idState = new IdentityFromRegState();
		idState.setSerializedConfiguration(state);
		return idState;
	}

	@Override
	protected ConfirmationStatus confirmElements(RegistrationRequest req, String state)
			throws EngineException
	{
		IdentityFromRegState idState = getState(state);
		if (!(identityTypesRegistry.getByName(idState.getType()).isVerifiable()))
			return new ConfirmationStatus(false, "ConfirmationStatus.identityChanged");
		System.out.println("Try confirm" + idState.getSerializedConfiguration());
		Collection<IdentityParam> confirmedList = confirmIdentity(req.getIdentities(),
				idState.getType(), idState.getValue());

		boolean confirmed = (confirmedList.size() > 0);
		return new ConfirmationStatus(confirmed,
				confirmed ? "ConfirmationStatus.successIdentity"
						: "ConfirmationStatus.identityChanged");

	}

	@Override
	public void updateSendedRequest(String state) throws EngineException
	{
		IdentityFromRegState idState = getState(state);
		String requestId = idState.getOwner();
		RegistrationRequestState reqState = internalRegistrationManagment
				.getRequest(requestId);
		for (IdentityParam id : reqState.getRequest().getIdentities())
		{

			updateConfirmationAmount(id, id.getValue());

		}
		SqlSession sql = db.getSqlSession(true);
		try
		{
			requestDB.update(requestId, reqState, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

}
