/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManUI;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.delegatedgroup.GroupAuthorizationRole;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays members grid with hamburger menu on the top
 * 
 * @author P.Piernik
 *
 */

public class GroupMembersComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private GroupMembersController controller;

	private GroupMemebersGrid groupMemebersGrid;
	private String group;
	private String project;

	public GroupMembersComponent(UnityMessageSource msg, GroupMembersController controller,
			String project) throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.project = project;
		Map<String, String> additionalProjectAttributes = controller
				.getAdditionalAttributeNamesForProject(project);

		setMargin(false);
		setSpacing(false);

		List<SingleActionHandler<GroupMemberEntry>> commonActions = new ArrayList<>();
		commonActions.add(getRemoveFromProjectAction());
		commonActions.add(getRemoveFromGroupAction());
		commonActions.add(getAddToGroupAction());

		List<SingleActionHandler<GroupMemberEntry>> rawActions = new ArrayList<>();
		rawActions.addAll(commonActions);
		rawActions.add(getAddManagerPrivilegesAction(true));
		rawActions.add(getRevokeManagerPrivilegesAction(true, s -> false));

		groupMemebersGrid = new GroupMemebersGrid(msg, rawActions,
				additionalProjectAttributes);

		HamburgerMenu<GroupMemberEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleNames(SidebarStyles.indentSmall.toString());
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		groupMemebersGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandlers(commonActions);
		hamburgerMenu.addActionHandler(getAddManagerPrivilegesAction(false));
		hamburgerMenu.addActionHandler(getRevokeManagerPrivilegesAction(false,
				s -> checkIfAllManagersSelected(s)));

		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu);
		addComponents(menuBar, groupMemebersGrid);
	}

	private SingleActionHandler<GroupMemberEntry> getRemoveFromProjectAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMembersComponent.removeFromProjectAction"))
				.withIcon(Images.removeFromGroup.getResource()).multiTarget()
				.withHandler(this::removeFromProject).build();
	}

	public void removeFromProject(Set<GroupMemberEntry> items)
	{
		removeFromGroup(project, items);
	}

	private SingleActionHandler<GroupMemberEntry> getRemoveFromGroupAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMembersComponent.removeFromGroupAction"))
				.withIcon(Images.deleteFolder.getResource()).multiTarget()
				.withHandler(this::removeFromGroup).build();
	}

	public void removeFromGroup(Set<GroupMemberEntry> items)
	{
		removeFromGroup(group, items);
	}

	private void removeFromGroup(String groupFrom, Set<GroupMemberEntry> items)
	{
		if (checkIfSelfProjectOperation(groupFrom, items))
		{
			new ConfirmDialog(msg, msg.getMessage(
					"GroupMembersComponent.confirmSelfRemoveFromProject",
					getProjectDisplayedNameSafe(project)), () -> {
						confirmedRemoveFromGroup(groupFrom, items);
						UpManUI.reloadProjects();
					}).show();
		} else
		{
			confirmedRemoveFromGroup(groupFrom, items);
			reloadMemebersGrid();
		}
	}

	private void confirmedRemoveFromGroup(String groupFrom, Set<GroupMemberEntry> items)
	{
		try
		{
			controller.removeFromGroup(project, groupFrom, items);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private SingleActionHandler<GroupMemberEntry> getAddToGroupAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMembersComponent.addToGroupAction"))
				.withIcon(Images.add.getResource()).multiTarget()
				.withHandler(this::showAddToGroupDialog).build();
	}

	private SingleActionHandler<GroupMemberEntry> getAddManagerPrivilegesAction(
			boolean hideIfInactive)
	{
		SingleActionHandler<GroupMemberEntry> handler = SingleActionHandler
				.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMembersComponent.addManagerPrivilegesAction"))
				.withIcon(Images.trending_up.getResource()).multiTarget()
				.withHandler(this::addManagerPrivileges)
				.withDisabledPredicate(e -> !e.getRole()
						.equals(GroupAuthorizationRole.regular))
				.build();
		handler.setHideIfInactive(hideIfInactive);
		return handler;
	}

	public void addManagerPrivileges(Set<GroupMemberEntry> items)
	{
		try
		{
			controller.addManagerPrivileges(project, items);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		reloadMemebersGrid();
	}

	private SingleActionHandler<GroupMemberEntry> getRevokeManagerPrivilegesAction(
			boolean hideIfInactive,
			Predicate<Set<GroupMemberEntry>> disabledCompositePredicate)
	{
		SingleActionHandler<GroupMemberEntry> handler = SingleActionHandler
				.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMembersComponent.revokeManagerPrivilegesAction"))
				.withIcon(Images.trending_down.getResource()).multiTarget()
				.withHandler(this::revokeManagerPrivileges)
				.withDisabledPredicate(e -> !e.getRole()
						.equals(GroupAuthorizationRole.manager)
						|| groupMemebersGrid.getManagersCount() < 2)
				.withDisabledCompositePredicate(disabledCompositePredicate).build();
		handler.setHideIfInactive(hideIfInactive);
		return handler;
	}

	private boolean checkIfAllManagersSelected(Set<GroupMemberEntry> items)
	{
		int selectedManagerCount = 0;
		for (GroupMemberEntry e : items)
		{
			if (e.getRole().equals(GroupAuthorizationRole.manager))
			{
				selectedManagerCount++;
			}
		}
		if (selectedManagerCount == groupMemebersGrid.getManagersCount())
		{
			return true;
		}
		return false;
	}

	public void revokeManagerPrivileges(Set<GroupMemberEntry> items)
	{
		if (checkIfSelfProjectOperation(project, items))
		{
			new ConfirmDialog(msg, msg.getMessage(
					"GroupMembersComponent.confirmSelfRevokeManagerPrivileges",
					getProjectDisplayedNameSafe(project)), () -> {

						confirmedRevokeManagerPrivileges(items);
						UpManUI.reloadProjects();
					}).show();

		} else
		{
			confirmedRevokeManagerPrivileges(items);
			reloadMemebersGrid();
		}
	}

	private void confirmedRevokeManagerPrivileges(Set<GroupMemberEntry> items)
	{
		try
		{
			controller.revokeManagerPrivileges(project, items);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	public void setGroup(String group)
	{
		this.group = group;
		reloadMemebersGrid();
	}

	private void reloadMemebersGrid()
	{
		List<GroupMemberEntry> groupMembers = new ArrayList<>();
		try
		{
			groupMembers.addAll(controller.getGroupMembers(project, group));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}

		groupMemebersGrid.setValue(groupMembers);
	}

	private boolean checkIfSelfProjectOperation(String group, Set<GroupMemberEntry> items)
	{
		boolean selfOperation = false;
		if (project.equals(group))
		{
			long managerId = InvocationContext.getCurrent().getLoginSession()
					.getEntityId();

			for (GroupMemberEntry e : items)
			{
				if (e.getEntityId() == managerId)
					selfOperation = true;
			}
		}
		return selfOperation;
	}

	private String getProjectDisplayedNameSafe(String projectPath)
	{
		String name = "";

		try
		{
			name = controller.getProjectGroupsMap(projectPath).get(projectPath);
		} catch (ControllerException e)
		{
			// ok
		}
		return name;
	}

	private void showAddToGroupDialog(Set<GroupMemberEntry> selection)
	{

		new TargetGroupSelectionDialog(msg, group -> {
			try
			{
				controller.addToGroup(project, group, selection);
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}
			reloadMemebersGrid();

		}).show();
	}

	private class TargetGroupSelectionDialog extends AbstractDialog
	{
		private Consumer<String> selectionConsumer;
		private GroupIndentCombo groupSelection;

		public TargetGroupSelectionDialog(UnityMessageSource msg,
				Consumer<String> selectionConsumer)
		{
			super(msg, msg.getMessage("AddToGroupDialog.caption"));
			this.selectionConsumer = selectionConsumer;
			setSizeEm(30, 18);
		}

		@Override
		protected FormLayout getContents()
		{
			Label info = new Label(msg.getMessage("AddToGroupDialog.info"));
			info.setWidth(100, Unit.PERCENTAGE);

			Map<String, String> groupsMap = new HashMap<>();
			try
			{
				groupsMap.putAll(controller.getProjectGroupsMap(project));
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}

			groupSelection = new GroupIndentCombo(
					msg.getMessage("AddToGroupDialog.selectGroup"), groupsMap);
			groupSelection.setWidth(100, Unit.PERCENTAGE);
			FormLayout main = new CompactFormLayout();
			main.addComponents(info, groupSelection);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			selectionConsumer.accept(groupSelection.getValue());
			close();
		}
	}
}
