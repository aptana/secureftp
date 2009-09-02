/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.io.sftp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.io.IVirtualFileManager;
import com.aptana.ide.core.io.IVirtualFileManagerDialog;
import com.aptana.ide.core.io.ProtocolManager;
import com.aptana.ide.core.io.sync.SyncManager;
import com.aptana.ide.core.ui.CoreUIPlugin;
import com.aptana.ide.core.ui.io.PasswordDialog;
import com.aptana.ide.io.ftp.FTPManagerDialogDelegate;
import com.aptana.ide.syncing.SyncingPlugin;
import com.aptana.ide.syncing.ftp.FtpDialog;

/**
 * @author Kevin Lindsey
 */
public class SftpProtocolManager extends ProtocolManager
{
	/*
	 * Fields
	 */
	private static Image fSFTPIcon;

	/**
	 * SftpProtocolManager
	 */
	static SftpProtocolManager _localProtocolManager = new SftpProtocolManager();

	/*
	 * Constructors
	 */

	/**
	 * static ctor
	 */
	static
	{
		ImageDescriptor imageDescriptor = SyncingPlugin.getImageDescriptor("icons/sftp.png"); //$NON-NLS-1$

		if (imageDescriptor != null)
		{
			fSFTPIcon = imageDescriptor.createImage();
		}
	}

	/*
	 * Methods
	 */

	/**
	 * getVirtualFileManagers
	 * 
	 * @return SftpVirtualFileManager[]
	 */
	public SftpVirtualFileManager[] getVirtualFileManagers()
	{
		SftpVirtualFileManager[] _cachedVirtualManagers = null;
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

		if (extensionRegistry != null)
		{
			IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(SyncingPlugin.ID,
					"sftpVirtualFileManagers"); //$NON-NLS-1$
			IExtension[] extensions = extensionPoint.getExtensions();
			List<SftpVirtualFileManager> found = new ArrayList<SftpVirtualFileManager>();

			for (int i = 0; i < extensions.length; i++)
			{
				IConfigurationElement[] configElements = extensions[i].getConfigurationElements();

				for (int j = 0; j < configElements.length; j++)
				{
					try
					{
						IConfigurationElement el = configElements[j];
						SftpVirtualFileManager pm = new SftpVirtualFileManager(this);

						if (pm != null)
						{
							found.add(pm);
							pm.setNickName(el.getAttribute("nickName")); //$NON-NLS-1$
							pm.setServer(el.getAttribute("host")); //$NON-NLS-1$
							pm.setUser(el.getAttribute("username")); //$NON-NLS-1$
							pm.setPassword(el.getAttribute("password")); //$NON-NLS-1$
							pm.setBasePath(StringUtils.EMPTY);
						}
					}
					catch (Exception ex)
					{
						IdeLog.logError(CoreUIPlugin.getDefault(), Messages.SftpProtocolManager_UnableToLoadFileManager, ex);
					}
				}
			}

			_cachedVirtualManagers = (SftpVirtualFileManager[]) found.toArray(new SftpVirtualFileManager[found.size()]);
		}

		return _cachedVirtualManagers;
	}

	/**
	 * SftpProtocolManager
	 */
	public SftpProtocolManager()
	{
		SftpVirtualFileManager[] virtualManagers = getVirtualFileManagers();

		if (virtualManagers != null)
		{
			for (int i = 0; i < virtualManagers.length; i++)
			{
				addFileManager(virtualManagers[i]);
			}
		}
	}

	/**
	 * /** getManagedType
	 * 
	 * @return String
	 */
	public String getManagedType()
	{
		return SftpVirtualFileManager.class.getName();
	}

	/**
	 * @see com.aptana.ide.core.io.ProtocolManager#createFileManager()
	 */
	public IVirtualFileManager createFileManager()
	{
		return createFileManager(true);
	}

	/**
	 * @see com.aptana.ide.core.io.ProtocolManager#createFileManager(boolean)
	 */
	public IVirtualFileManager createFileManager(boolean addManager)
	{
		SftpVirtualFileManager vfm = new SftpVirtualFileManager(this);
		vfm.setPasswordListener(new PasswordDialog());
		if(addManager)
		{
			addFileManager(vfm);
		}
		return vfm;
	}

	/**
	 * @see com.aptana.ide.core.io.ProtocolManager#getImage()
	 */
	public Image getImage()
	{
		return fSFTPIcon;
	}

	/**
	 * getFileManagers
	 * 
	 * @return IVirtualFileManager[]
	 */
	public IVirtualFileManager[] getFileManagers()
	{
		return (IVirtualFileManager[]) SyncManager.getSyncManager().getItems(SftpVirtualFileManager.class);
	}

	/**
	 * @see ProtocolManager#createPropertyDialog(Shell, int)
	 */
	public IVirtualFileManagerDialog createPropertyDialog(Shell parent, int style)
	{
		return new FTPManagerDialogDelegate(new FtpDialog(parent));
	}

	/**
	 * getInstance
	 * 
	 * @return SftpProtocolManager
	 */
	public static SftpProtocolManager getInstance()
	{
		return _localProtocolManager;
	}

	/**
	 * @see ProtocolManager#getStaticInstance()
	 */
	public ProtocolManager getStaticInstance()
	{
		return getInstance();
	}

}
