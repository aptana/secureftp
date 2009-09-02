/**
 * This file Copyright (c) 2005-2008 Aptana, Inc. This program is
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

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 * @author Ingo Muschenetz
 *
 */
public final class Messages extends NLS
{
	private static final String BUNDLE_NAME = "com.aptana.ide.io.sftp.messages"; //$NON-NLS-1$

	private Messages()
	{
	}

	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * SftpProtocolManager_UnableToLoadFileManager
	 */
	public static String SftpProtocolManager_UnableToLoadFileManager;

	/**
	 * SftpVirtualFileManager_Cannot_Read_File
	 */
	public static String SftpVirtualFileManager_Cannot_Read_File;

	/**
	 * SftpVirtualFileManager_FailedToGetAuthenticationMode
	 */
	public static String SftpVirtualFileManager_FailedToGetAuthenticationMode;

	/**
	 * SftpVirtualFileManager_PathCannotBeEmpty
	 */
	public static String SftpVirtualFileManager_PathCannotBeEmpty;

	/**
	 * SftpVirtualFileManager_CannotSetRemoteFile
	 */
	public static String SftpVirtualFileManager_CannotSetRemoteFile;

	/**
	 * SftpVirtualFileManager_SFTPPasswordPrompt
	 */
	public static String SftpVirtualFileManager_SFTPPasswordPrompt;

	/**
	 * SftpVirtualFileManager_AuthenticationFailed
	 */
	public static String SftpVirtualFileManager_AuthenticationFailed;

	/**
	 * SftpVirtualFileManager_UnsupportedAuthenticationType
	 */
	public static String SftpVirtualFileManager_UnsupportedAuthenticationType;

	/**
	 * SftpVirtualFileManager_AuthenticationSucceededAnotherRequired
	 */
	public static String SftpVirtualFileManager_AuthenticationSucceededAnotherRequired;

	/**
	 * SftpVirtualFileManager_InvalidHostFile
	 */
	public static String SftpVirtualFileManager_InvalidHostFile;

	/**
	 * SftpVirtualFileManager_AnableToConnect
	 */
	public static String SftpVirtualFileManager_AnableToConnect;

	/**
	 * SftpVirtualFileManager_UnableToCreateLocalDirectory
	 */
	public static String SftpVirtualFileManager_UnableToCreateLocalDirectory;

	/**
	 * SftpVirtualFileManager_UnableToDeleteFile
	 */
	public static String SftpVirtualFileManager_UnableToDeleteFile;

	/**
	 * SftpVirtualFileManager_Downloading
	 */
	public static String SftpVirtualFileManager_Downloading;

	/**
	 * SftpVirtualFileManager_UnableToGetStream
	 */
	public static String SftpVirtualFileManager_UnableToGetStream;

	/**
	 * SftpVirtualFileManager_UnableToDownload
	 */
	public static String SftpVirtualFileManager_UnableToDownload;

	/**
	 * SftpVirtualFileManager_UnableToRefresh
	 */
	public static String SftpVirtualFileManager_UnableToRefresh;

	/**
	 * SftpVirtualFileManager_NewNameMustNotBePath
	 */
	public static String SftpVirtualFileManager_NewNameMustNotBePath;

	/**
	 * SftpVirtualFileManager_UnableToRenameFile
	 */
	public static String SftpVirtualFileManager_UnableToRenameFile;

	/**
	 * SftpVirtualFileManager_InputStreamCannotBeNull
	 */
	public static String SftpVirtualFileManager_InputStreamCannotBeNull;

	/**
	 * SftpVirtualFileManager_Uploading
	 */
	public static String SftpVirtualFileManager_Uploading;

	/**
	 * SftpVirtualFileManager_UnableToUploadStream
	 */
	public static String SftpVirtualFileManager_UnableToUploadStream;

	/**
	 * SftpVirtualFileManager_Success
	 */
	public static String SftpVirtualFileManager_Success;

	/**
	 * SftpVirtualFileManager_ERR_setModTime
	 */
	public static String SftpVirtualFileManager_ERR_setModTime;

	/**
	 * SftpVirtualFileManager_ERR_setModTime1
	 */
    public static String SftpVirtualFileManager_ERR_setModTime1;

    /**
	 * SftpVirtualFileManager_Error
	 */
	public static String SftpVirtualFileManager_Error;

	public static String SftpVirtualFileManager_ERR_PrivateKeyFailedWithNoPassword;

}

