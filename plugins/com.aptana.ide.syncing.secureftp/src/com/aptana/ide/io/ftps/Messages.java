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
package com.aptana.ide.io.ftps;

import org.eclipse.osgi.util.NLS;

/**
 * @author Ingo Muschenetz
 */
public final class Messages extends NLS
{
	private static final String BUNDLE_NAME = "com.aptana.ide.io.ftps.messages"; //$NON-NLS-1$

	private Messages()
	{
	}

	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * FtpsVirtualFileManager_Cannot_Get_Time_String
	 */
	public static String FtpsVirtualFileManager_Cannot_Get_Time_String;

	/**
	 * FtpsVirtualFileManager_Cannot_Read_File
	 */
	public static String FtpsVirtualFileManager_Cannot_Read_File;

	/**
	 * FtpsVirtualFileManager_PathCannotBeEmpty
	 */
	public static String FtpsVirtualFileManager_PathCannotBeEmpty;

	/**
	 * FtpsVirtualFileManager_UnableToGetFiles
	 */
	public static String FtpsVirtualFileManager_UnableToGetFiles;

	/**
	 * FtpsVirtualFileManager_CouldNotSetRemoteFile
	 */
	public static String FtpsirtualFileManager_CouldNotSetRemoteFile;

	/**
	 * FtpsVirtualFileManager_UnableToConnect
	 */
	public static String FtpsVirtualFileManager_UnableToConnect;

	/**
	 * FtpsVirtualFileManager_UnableToCreateDirectory
	 */
	public static String FtpsVirtualFileManager_UnableToCreateDirectory;

	/**
	 * FtpsVirtualFileManager_UnableToDelete
	 */
	public static String FtpsVirtualFileManager_UnableToDelete;

	/**
	 * FtpsVirtualFileManager_UnableToDisconnect
	 */
	public static String FtpsVirtualFileManager_UnableToDisconnect;

	/**
	 * FtpsVirtualFileManager_Downloading
	 */
	public static String FtpsVirtualFileManager_Downloading;

	/**
	 * FtpsVirtualFileManager_Success
	 */
	public static String FtpsVirtualFileManager_Success;

	/**
	 * FtpsVirtualFileManager_ERR_setModTime
	 */
	public static String FtpsVirtualFileManager_ERR_setModTime;

	/**
	 * FtpsVirtualFileManager_ERR_setModTime1
	 */
    public static String FtpsVirtualFileManager_ERR_setModTime1;

    /**
	 * FtpsVirtualFileManager_Error
	 */
	public static String FtpsVirtualFileManager_Error;

	/**
	 * FtpsVirtualFileManager_UnableToGetStream
	 */
	public static String FtpsVirtualFileManager_UnableToGetStream;

	/**
	 * FtpsVirtualFileManager_InputCannotBeNull
	 */
	public static String FtpsVirtualFileManager_InputCannotBeNull;
	
	/**
	 * FtpsVirtualFileManager_Unsupported_Date_Time_Format;
	 */
	public static String FtpsVirtualFileManager_Unsupported_Date_Time_Format;

	/**
	 * FtpsVirtualFileManager_Uploading
	 */
	public static String FtpsVirtualFileManager_Uploading;

	/**
	 * FtpsVirtualFileManager_UnableToUpload
	 */
	public static String FtpsVirtualFileManager_UnableToUpload;

	/**
	 * FtpsVirtualFileManager_UnableToDownload
	 */
	public static String FtpsVirtualFileManager_UnableToDownload;

	/**
	 * FtpsVirtualFileManager_UnableToRefresh
	 */
	public static String FtpsVirtualFileManager_UnableToRefresh;

	/**
	 * FtpsVirtualFileManager_NewNameNotAPath
	 */
	public static String FtpsVirtualFileManager_NewNameNotAPath;

	/**
	 * FtpsVirtualFileManager_UnableToRename
	 */
	public static String FtpsVirtualFileManager_UnableToRename;

	/**
	 * FtpsVirtualFileManager_FTPPasswordPrompt
	 */
	public static String FtpsVirtualFileManager_FTPPasswordPrompt;
}
