/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.util.List;

import io.imunity.upman.common.UpManGrid;
import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectRequest.RequestOperation;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Displays a grid with update requests
 * 
 * @author P.Piernik
 *
 */

public class UpdateRequestsGrid extends UpManGrid<UpdateRequestEntry>
{

	enum BaseColumn
	{
		operation("UpdateRequest.operation"), name("UpdateRequest.name"), email("UpdateRequest.email"), groups(
				"UpdateRequest.groups"), requested(
						"UpdateRequest.requested"), action("UpdateRequest.action");

		private String captionKey;

		BaseColumn(String captionKey)
		{
			this.captionKey = captionKey;
		}
	};

	public UpdateRequestsGrid(UnityMessageSource msg,
			List<SingleActionHandler<UpdateRequestEntry>> rowActionHandlers)
	{
		super(msg, (UpdateRequestEntry e) -> e.id);
		createColumns(rowActionHandlers);
	}

	private void createBaseColumns()
	{

		addColumn(r -> r.operation.equals(RequestOperation.SelfSignUp)
				? msg.getMessage("UpdateRequest.selfSignUp") : msg.getMessage("UpdateRequest.update") ).setCaption(msg.getMessage(BaseColumn.operation.captionKey))
				.setExpandRatio(3);
		addColumn(r -> r.name).setCaption(msg.getMessage(BaseColumn.name.captionKey)).setExpandRatio(3);
		addColumn(r -> r.email).setCaption(msg.getMessage(BaseColumn.email.captionKey)).setExpandRatio(3);

		UpManGridHelper.createGroupsColumn(this, (UpdateRequestEntry e) -> e.groupsDisplayedNames,
				msg.getMessage(BaseColumn.groups.captionKey));

		UpManGridHelper.createDateTimeColumn(this, (UpdateRequestEntry e) -> e.requestedTime,
				msg.getMessage(BaseColumn.requested.captionKey));

	}

	private void createColumns(List<SingleActionHandler<UpdateRequestEntry>> rowActionHandlers)
	{
		createBaseColumns();
		UpManGridHelper.createActionColumn(this, rowActionHandlers, msg.getMessage(BaseColumn.action.captionKey));
	}

}
