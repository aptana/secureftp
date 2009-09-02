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
package com.aptana.ide.syncing.sftp;

import org.eclipse.osgi.util.NLS;

/**
 * @author Kevin Sawicki (ksawicki@aptana.com)
 */
public class Messages extends NLS
{
	private static final String BUNDLE_NAME = "com.aptana.ide.syncing.sftp.messages"; //$NON-NLS-1$

	/**
	 * SftpInfoDialog_LBL_All
	 */
	public static String SftpInfoDialog_LBL_All;

    /**
     * SftpInfoDialog_LBL_Execute
     */
    public static String SftpInfoDialog_LBL_Execute;

    /**
     * SftpInfoDialog_LBL_Group
     */
    public static String SftpInfoDialog_LBL_Group;

    /**
     * SftpInfoDialog_LBL_Owner
     */
    public static String SftpInfoDialog_LBL_Owner;

    /**
     * SftpInfoDialog_LBL_OwnerGroup
     */
    public static String SftpInfoDialog_LBL_OwnerGroup;

    /**
     * SftpInfoDialog_LBL_Permissions
     */
    public static String SftpInfoDialog_LBL_Permissions;

    /**
     * SftpInfoDialog_LBL_Read
     */
    public static String SftpInfoDialog_LBL_Read;

    /**
     * SftpInfoDialog_LBL_User
     */
    public static String SftpInfoDialog_LBL_User;

    /**
     * SftpInfoDialog_LBL_Write
     */
    public static String SftpInfoDialog_LBL_Write;

	/**
	 * InfoDialog_Info
	 */
	public static String InfoDialog_Info;

	/**
	 * InfoDialog_General
	 */
	public static String InfoDialog_General;

	/**
	 * InfoDialog_Kind
	 */
	public static String InfoDialog_Kind;

	/**
	 * InfoDialog_Size
	 */
	public static String InfoDialog_Size;

	/**
	 * InfoDialog_Where
	 */
	public static String InfoDialog_Where;

	/**
	 * InfoDialog_Modified
	 */
	public static String InfoDialog_Modified;

	/**
	 * InfoDialog_Folder
	 */
	public static String InfoDialog_Folder;

	/**
	 * InfoDialog_File
	 */
	public static String InfoDialog_File;

	/**
	 * InfoDialog_Bytes
	 */
	public static String InfoDialog_Bytes;
	
	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}
