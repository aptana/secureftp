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

package com.aptana.ide.filesystem.secureftp;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.eclipse.jsch.internal.core.PreferenceInitializer;

import com.aptana.ide.core.StringUtils;
import com.enterprisedt.net.j2ssh.transport.publickey.InvalidSshKeyException;
import com.enterprisedt.net.j2ssh.transport.publickey.SshPrivateKeyFile;
import com.enterprisedt.net.puretls.LoadProviders;

/**
 * @author Max Stepanov
 *
 */
@SuppressWarnings("restriction")
public final class SecureUtils {

	/**
	 * 
	 */
	private SecureUtils() {
	}

	public static boolean isKeyPassphraseProtected(File keyFile) throws CoreException {
		try {
			LoadProviders.init();
			SshPrivateKeyFile privateKeyFile = SshPrivateKeyFile.parse(keyFile);
			return privateKeyFile.isPassphraseProtected();
		} catch (InvalidSshKeyException e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Private Key file {0} is invalid", keyFile.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Private Key file {0} cannot be read", keyFile.getAbsolutePath())));
		}
	}
	
	public static boolean isPassphraseValid(File keyFile, char[] password) {
		try {
			SshPrivateKeyFile.parse(keyFile).toPrivateKey(String.copyValueOf(password));
			return true;
		} catch (InvalidSshKeyException e) {
		} catch (IOException e) {
		}
		return false;
	}
	
	public static String getSSH_HOME() {
		return Platform.getPreferencesService().getString(JSchCorePlugin.ID, IConstants.KEY_SSH2HOME, PreferenceInitializer.SSH_HOME_DEFAULT, null);
	}

	public static String[] getPrivateKeys() {
		String value = Platform.getPreferencesService().getString(JSchCorePlugin.ID, IConstants.KEY_PRIVATEKEY, IConstants.PRIVATE_KEYS_DEFAULT, null);
		if (value != null && value.length() > 0) {
			return value.trim().split(",");
		}
		return new String[0];
	}

}
