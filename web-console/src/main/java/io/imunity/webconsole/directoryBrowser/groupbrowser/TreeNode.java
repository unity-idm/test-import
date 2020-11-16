/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupbrowser;

import com.vaadin.data.TreeData;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;

class TreeNode
{
	private String name;
	private String path;
	private TreeNode parent;
	private MessageSource msg;
	private boolean delegated;

	TreeNode(MessageSource msg, Group group)
	{
		this(msg, group, null);
	}

	TreeNode(MessageSource msg, Group group, TreeNode parent)
	{
		this.msg = msg;
		this.path = group.toString();
		this.parent = parent;
		setGroupMetadata(group);
	}

	void setGroupMetadata(Group group)
	{
		this.setDelegated(group.getDelegationConfiguration().enabled);
		if (group.isTopLevel())
		{
			this.name = group.getDisplayedName().getValue(msg);
			if (this.name.equals("/"))
				this.name = msg.getMessage("GroupBrowser.root");
			else
				this.name = name + " (/)";
		} else
		{
			this.name = group.getDisplayedName().getValue(msg);
			String realName = group.toString();
			if (!realName.equals(name))
				this.name = name + " (" + realName + ")";
		}
	}
	
	public boolean isDelegated()
	{
		return delegated;
	}

	public void setDelegated(boolean delegated)
	{
		this.delegated = delegated;
	}
	
	TreeNode getParentNode()
	{
		return parent;
	}

	String getPath()
	{
		return path;
	}

	void setPath(String path)
	{
		this.path = path;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public int hashCode()
	{

		return path.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof String)
			return path.equals(obj);
		if (obj instanceof TreeNode)
			return path.equals(((TreeNode) obj).path);
		return false;
	}

	boolean anyFieldsOrChildContains(String searched, MessageSource msg, TreeData<TreeNode> treeData)
	{
		String textLower = searched.toLowerCase();

		if (toString().toLowerCase().contains(textLower))
			return true;

		if (path.toLowerCase().contains(textLower))
			return true;

		boolean anyChildContains = false;

		for (TreeNode child : treeData.getChildren(this))
		{
			anyChildContains |= child.anyFieldsOrChildContains(searched, msg, treeData);
		}

		return anyChildContains;
	}

	boolean isChild(TreeNode parent)
	{
		return Group.isChild(getPath(), parent.path);
	}
}