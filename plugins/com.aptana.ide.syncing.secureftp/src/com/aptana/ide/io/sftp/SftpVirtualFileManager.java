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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.PluginUtils;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.io.ConnectionException;
import com.aptana.ide.core.io.IFileProgressMonitor;
import com.aptana.ide.core.io.IPasswordListener;
import com.aptana.ide.core.io.IVirtualFile;
import com.aptana.ide.core.io.IVirtualFileManager;
import com.aptana.ide.core.io.ProtocolManager;
import com.aptana.ide.core.io.VirtualFileManagerException;
import com.aptana.ide.core.io.VirtualManagerBase;
import com.aptana.ide.core.io.sync.ISerializableSyncItem;
import com.aptana.ide.core.ui.CoreUIPlugin;
import com.aptana.ide.core.ui.CoreUIUtils;
import com.aptana.ide.core.ui.io.file.FilePrefUtils;
import com.aptana.ide.io.ftp.IFtpVirtualFileManager;
import com.aptana.ide.syncing.SyncingPlugin;
import com.aptana.ide.syncing.ftp.FtpDialogFactory;
import com.aptana.ide.syncing.sftp.SftpDialogFactory;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.ssh.SSHFTPClient;
import com.enterprisedt.net.j2ssh.SshException;
import com.enterprisedt.net.j2ssh.transport.publickey.InvalidSshKeyException;

/**
 * @author Kevin Lindsey
 */
public class SftpVirtualFileManager extends VirtualManagerBase implements IFtpVirtualFileManager
{
	private static Image fSFTPIcon;
	private static Image fSFTPDisabledIcon;

	private String _server = StringUtils.EMPTY;
	private String _user = StringUtils.EMPTY;
	private String _keyFile = StringUtils.EMPTY;
	private String _password = StringUtils.EMPTY;
	private String _basePath = StringUtils.EMPTY;
	private int _port = 22;
	private Image _image;
	private Image _disabledImage;
	private long _lastEnsuredConnection;

	private SSHFTPClient _sftpClient;
	
	private String descriptiveLabel;

	private IPasswordListener _passwordListener;
	private boolean _savePassword;

	/**
	 * static constructor
	 */
	static
	{
		ImageDescriptor imageDescriptor = SyncingPlugin.getImageDescriptor("icons/sftp.gif"); //$NON-NLS-1$
		ImageDescriptor disabledImageDescriptor = SyncingPlugin.getImageDescriptor("icons/sftp_disabled.gif"); //$NON-NLS-1$

		if (imageDescriptor != null)
		{
			fSFTPIcon = imageDescriptor.createImage();
			fSFTPDisabledIcon = disabledImageDescriptor.createImage();
		}
	}

	/**
	 * SftpFileManager
	 * 
	 * @param protocolManager
	 */
	SftpVirtualFileManager(ProtocolManager protocolManager)
	{
		super(protocolManager);

		// set default base path
		this._basePath = StringUtils.EMPTY;

		this._savePassword = true;
		
		this.descriptiveLabel = ""; //$NON-NLS-1$
	}

	/**
	 * ensures we have a valid FTP connection
	 */
	private void ensureConnection() throws ConnectionException
	{
		if (isConnected() == false)
		{
			this.connect();
		}
		else
		{
			long elapsedMillis = System.currentTimeMillis() - this._lastEnsuredConnection;
			long timeoutMillis = this._sftpClient.getTimeout();
			// long timeoutMillis = 10L * 60L * 1000L;
			//			
			// System.out.println("timeout millis = " + timeoutMillis);
			// System.out.println("elapsed millis = " + elapsedMillis);

			if (elapsedMillis > timeoutMillis)
			{
				this.disconnect();
				this.connect();
			}
		}

		this._lastEnsuredConnection = System.currentTimeMillis();
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#isEditable()
	 */
	public boolean isEditable()
	{
		return true;
	}
	
	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getBaseFile()
	 */
	public IVirtualFile getBaseFile()
	{
		return this.createVirtualDirectory(this._basePath);
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getBasePath()
	 */
	public String getBasePath()
	{
		return this._basePath;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#setBasePath(java.lang.String)
	 */
	public void setBasePath(String path)
	{
		if (path == null || path.length() == 0)
		{
			throw new IllegalArgumentException(Messages.SftpVirtualFileManager_PathCannotBeEmpty);
		}

		this._basePath = path;
	}

	/**
	 * isConnected
	 * 
	 * @return boolean
	 */
	public boolean isConnected()
	{
		return (this._sftpClient != null && this._sftpClient.connected());
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getFileTimeString(com.aptana.ide.core.io.IVirtualFile)
	 */
	public String getFileTimeString(IVirtualFile file)
	{
		return file.getTimeStamp();
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getFiles(com.aptana.ide.core.io.IVirtualFile, boolean, boolean)
	 */
	public IVirtualFile[] getFiles(IVirtualFile file, boolean recurse, boolean includeCloakedFiles)
			throws ConnectionException, IOException
	{
		List<IVirtualFile> list = new ArrayList<IVirtualFile>();

		if (file.canRead())
		{
			this.getFiles(file.getAbsolutePath(), recurse, list, includeCloakedFiles);
		}

		return list.toArray(new IVirtualFile[list.size()]);
	}

	/**
	 * @see com.aptana.ide.core.io.VirtualManagerBase#getDescriptiveLabel()
	 */
	public String getDescriptiveLabel()
	{
		if (this.descriptiveLabel != null && this.descriptiveLabel.length() > 0)
		{
			return this.descriptiveLabel;
		}
		else
		{
			return super.getDescriptiveLabel();
		}
	}

	/**
	 * @see com.aptana.ide.core.io.VirtualManagerBase#getNickName()
	 */
	public String getNickName()
	{
		if (this.descriptiveLabel != null && this.descriptiveLabel.length() > 0)
		{
			return this.descriptiveLabel;
		}
		return super.getNickName();
	}
	
	/**
	 * getFiles
	 * 
	 * @param path
	 * @param list
	 * @throws ConnectionException
	 * @throws IOException
	 */
	private synchronized void getFiles(String path, boolean recurse, List<IVirtualFile> list, boolean includeCloakedFiles)
			throws ConnectionException, IOException
	{
		ensureConnection();

		// fire event
		if (!this.fireGetFilesEvent(path))
		{
			return;
		}

		try
		{
			FTPFile[] files = this._sftpClient.dirDetails(path);
			FTPFile file;

			for (int i = 0; i < files.length; i++)
			{
				file = files[i];

				String filePath = path + this.getFileSeparator() + file.getName();

				// correct root directory case
				if (filePath.startsWith("//")) //$NON-NLS-1$
				{
					filePath = filePath.substring(1);
				}
				
				boolean isLink = file.isLink() || file.getRaw().startsWith("l"); //$NON-NLS-1$

				// don't allow "." and ".."
				if (validName(file.getName()))
				{
					boolean isDirectory = false;

					if (isLink)
					{
						if (file.isDir())
						{
							isDirectory = true;
						}
						else
						{
							// make sure this is really a file
							try
							{
								FTPFile[] f = this._sftpClient.dirDetails(filePath);
								isDirectory = f.length > 1;
							}
							catch (Exception e)
							{
								// fail silently
							}
						}
					}
					else
					{
						isDirectory = file.isDir();
					}
					
					long date = file.lastModified().getTime();
					long permissions = this.permissionStringToLong(file.getRaw().substring(0, 10));
					String owner = file.getOwner();
					String group = file.getGroup();

					// create virtual file
					SftpVirtualFile virtualFile = new SftpVirtualFile(this, filePath, permissions, date, isDirectory,
							file.size());
					virtualFile.setTimeStamp(file.lastModified().toString());
					virtualFile.internalSetOwner(owner);
					virtualFile.internalSetGroup(group);
					virtualFile.setIsLink(isLink);

					if (includeCloakedFiles || virtualFile.isCloaked() == false)
					{
						// add virtual file to result
						list.add(virtualFile);

						// descend into directories if this call is recursive
						if (recurse && isDirectory && virtualFile.canRead() && isLink == false)
						{
							this.getFiles(virtualFile.getAbsolutePath(), recurse, list, includeCloakedFiles);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new VirtualFileManagerException(e);
		}
	}

	/**
	 * @throws IOException
	 * @see com.aptana.ide.core.io.IVirtualFileManager#hasFiles(com.aptana.ide.core.io.IVirtualFile)
	 */
	public boolean hasFiles(IVirtualFile file) throws ConnectionException, IOException
	{
		return file.getFiles().length > 0;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getFileSeparator()
	 */
	public String getFileSeparator()
	{
		return "/"; //$NON-NLS-1$
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getGroup(com.aptana.ide.core.io.IVirtualFile)
	 */
	public String getGroup(IVirtualFile file)
	{
		return null;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#setGroup(com.aptana.ide.core.io.IVirtualFile, java.lang.String)
	 */
	public void setGroup(IVirtualFile file, String groupName)
	{
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getImage()
	 */
	public Image getImage()
	{
		if (this._image == null)
		{
			this._image = fSFTPIcon;
		}

		return this._image;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#setImage(org.eclipse.swt.graphics.Image)
	 */
	public void setImage(Image image)
	{
		this._image = image;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getDisabledImage()
	 */
	public Image getDisabledImage()
	{
		if (_disabledImage == null)
		{
			return fSFTPDisabledIcon;
		}
		else
		{
			return _disabledImage;
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#setDisabledImage(org.eclipse.swt.graphics.Image)
	 */
	public void setDisabledImage(Image image)
	{
		_disabledImage = image;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getOwner(com.aptana.ide.core.io.IVirtualFile)
	 */
	public String getOwner(IVirtualFile file)
	{
		return null;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#setOwner(com.aptana.ide.core.io.IVirtualFile, java.lang.String)
	 */
	public void setOwner(IVirtualFile file, String ownerName)
	{
	}

	/**
	 * Get password
	 * 
	 * @return The password used to connect to this file manager's server
	 */
	public String getPassword()
	{
		return this._password;
	}

	/**
	 * getPasswordListener
	 * 
	 * @return IPasswordListener
	 */
	public IPasswordListener getPasswordListener()
	{
		return this._passwordListener;
	}

	/**
	 * setPasswordListener
	 * 
	 * @param listener
	 */
	public void setPasswordListener(IPasswordListener listener)
	{
		this._passwordListener = listener;
	}

	/**
	 * setPassword
	 * 
	 * @param password
	 */
	public void setPassword(String password)
	{
		this._password = password;
	}

	/**
	 * getPort
	 * 
	 * @return Returns the port.
	 */
	public int getPort()
	{
		return this._port;
	}

	/**
	 * setPort
	 * 
	 * @param port
	 *            The port to set.
	 */
	public void setPort(int port)
	{
		this._port = port;
	}

	/**
	 * getSavePassword
	 * 
	 * @return boolean
	 */
	public boolean getSavePassword()
	{
		return this._savePassword;
	}

	/**
	 * setSavePassword
	 * 
	 * @param value
	 */
	public void setSavePassword(boolean value)
	{
		this._savePassword = value;
	}

	/**
	 * Get server
	 * 
	 * @return The server managed by this file manager
	 */
	public String getServer()
	{
		return this._server;
	}

	/**
	 * setServer
	 * 
	 * @param server
	 */
	public void setServer(String server)
	{
		this._server = server;
	}

	/**
	 * Get user
	 * 
	 * @return The user name used to connect to this file manager's server
	 */
	public String getUser()
	{
		return this._user;
	}

	/**
	 * setUser
	 * 
	 * @param user
	 */
	public void setUser(String user)
	{
		this._user = user;
	}

	/**
	 * connect
	 * 
	 * @throws ConnectionException
	 */
	public synchronized void connect() throws ConnectionException
	{
		if (this.isConnected() == false)
		{
			this._sftpClient = new SSHFTPClient();

			try
			{
			    this._sftpClient.setControlEncoding(CoreUIUtils.getFileEncoding(null));
				this._sftpClient.setRemoteHost(this.getServer());
				this._sftpClient.setRemotePort(this.getPort());
				this._sftpClient.getValidator().setHostValidationEnabled(false);
				if (this.getPrivateKeyFile() != null && this.getPrivateKeyFile().trim().length() > 0)
				{
					this._sftpClient.setAuthentication(this.getPrivateKeyFile(), this.getUser(), this.getPassword());
				}
				else
				{
					this._sftpClient.setAuthentication(this.getUser(), this.getPassword());
				}
				this._sftpClient.connect();
			}
			catch (Exception e)
			{
				if (this.isConnected())
				{
					this.disconnect();
				}
				// special case for when user hasn't specified password, is using key auth and it failed (lib reports I/O error, but it's really because we need password)
				if (e instanceof InvalidSshKeyException && (getPassword() == null || getPassword().trim().length() == 0))
				{
					throw new ConnectionException(Messages.SftpVirtualFileManager_ERR_PrivateKeyFailedWithNoPassword);
				}
				
				throw new ConnectionException(e);
			}
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#createLocalDirectory(com.aptana.ide.core.io.IVirtualFile)
	 */
	public synchronized boolean createLocalDirectory(IVirtualFile directoryFile) throws ConnectionException,
			VirtualFileManagerException
	{
		String basePath = this.getBaseFile().getAbsolutePath();
		String directoryPath = directoryFile.getAbsolutePath();
		boolean result = false;

		if (basePath.equals(directoryPath))
		{
			// assume base directories exist

			// NOTE: directoryFile.exists() will fail in FTP since we have to get the parent file and then list its
			// contents
			// to determine if the directory exists.
			result = true;
		}
		else
		{
			ensureConnection();

			try
			{
				try
				{
					mkdirs(directoryFile);
				}
				catch (FTPException e)
				{
					IdeLog.logError(SyncingPlugin.getDefault(),
							"Unexpected FTP exception during ftp folder creation ", e); //$NON-NLS-1$
					throw e;
				}
				catch (IOException e)
				{
					IdeLog.logError(SyncingPlugin.getDefault(),
							"Unexpected IO exception during ftp folder creation ", e); //$NON-NLS-1$
					throw e;
				}

				result = true;
			}
			catch (Exception e)
			{
				throw new VirtualFileManagerException(StringUtils.format(
						Messages.SftpVirtualFileManager_UnableToCreateLocalDirectory, directoryFile.getAbsolutePath()),
						e);
			}
		}

		return result;
	}
	
	private void mkdirs(IVirtualFile directoryFile) throws ConnectionException, IOException, FTPException {
		String basePath = this.getBaseFile().getAbsolutePath();
		String directoryPath = directoryFile.getAbsolutePath();
		if (basePath.equals(directoryPath)) {
			// assume base directories exist
			return;
		}
		if (!directoryFile.exists()) {
			mkdirs(directoryFile.getParentFile());
			this._sftpClient.mkdir(directoryFile.getAbsolutePath());
			// Set directories to the directory permissions defined in the preferences
			directoryFile.setPermissions(FilePrefUtils.getDirectoryPermission());
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#createVirtualDirectory(java.lang.String)
	 */
	public IVirtualFile createVirtualDirectory(String path)
	{
		return new SftpVirtualFile(this, path, true);
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#createVirtualFile(java.lang.String)
	 */
	public IVirtualFile createVirtualFile(String path)
	{
		return new SftpVirtualFile(this, path);
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#deleteFile(com.aptana.ide.core.io.IVirtualFile)
	 */
	public synchronized boolean deleteFile(IVirtualFile file) throws ConnectionException, VirtualFileManagerException
	{
		boolean result = false;

		ensureConnection();

		try
		{
			this._sftpClient.chdir("/");
			this._sftpClient.delete(file.getAbsolutePath());
			result = true;
		}
		catch (Exception e)
		{
			throw new VirtualFileManagerException(StringUtils.format(
					Messages.SftpVirtualFileManager_UnableToDeleteFile, file.getAbsolutePath()), e);
		}

		return result;
	}

	/**
	 * disconnect
	 */
	public synchronized void disconnect()
	{
		if (this.isConnected())
		{
			try
			{
				this._sftpClient.quit();
			}
			catch (Exception e)
			{
			}

			this._sftpClient = null;
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getStream(com.aptana.ide.core.io.IVirtualFile)
	 */
	public synchronized InputStream getStream(IVirtualFile file) throws ConnectionException, VirtualFileManagerException
	{
		byte[] data = null;

		try
		{
			data = this._sftpClient.get(file.getAbsolutePath());
		}
		catch (Exception e)
		{
			throw new VirtualFileManagerException(e);
		}
		finally
		{
			if (data == null)
			{
				data = new byte[0];
			}
		}

		return new ByteArrayInputStream(data);
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#putToLocalFile(com.aptana.ide.core.io.IVirtualFile, java.io.File)
	 */
	public synchronized void putToLocalFile(IVirtualFile file, File tempFile) throws ConnectionException,
			VirtualFileManagerException
	{
		if (file.canRead())
		{
			int tries = 2;

			while (tries > 0)
			{
				ensureConnection();

				try
				{
					this._sftpClient.get(tempFile.getAbsolutePath(), file.getAbsolutePath());
					tries = 0;
				}
				catch (SshException e)
				{
					if ("The SFTP connection has been closed".equals(e.getMessage()) && tries > 1) //$NON-NLS-1$
					{
						// unfortunately we are not getting reliable connect info. In this case ensureConnection
						// thought we were connected, but we weren't, so we force a disconnect and try again
						this.disconnect();

						// Let's try this approach once
						tries--;
					}
					else
					{
						throw new VirtualFileManagerException(e);
					}
				}
				catch (Exception e)
				{
					throw new VirtualFileManagerException(e);
				}
			}
		}
		else
		{
			throw new VirtualFileManagerException(Messages.SftpVirtualFileManager_Cannot_Read_File);
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#moveFile(com.aptana.ide.core.io.IVirtualFile,
	 *      com.aptana.ide.core.io.IVirtualFile)
	 */
	public boolean moveFile(IVirtualFile source, IVirtualFile destination)
	{
		return false;
	}

	/**
	 * refresh
	 */
	public void refresh()
	{
		disconnect();

		try
		{
			connect();
		}
		catch (Exception e)
		{
			IdeLog.logError(SyncingPlugin.getDefault(), Messages.SftpVirtualFileManager_UnableToRefresh, e);
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#renameFile(com.aptana.ide.core.io.IVirtualFile, java.lang.String)
	 */
	public synchronized boolean renameFile(IVirtualFile file, String newName) throws ConnectionException,
			VirtualFileManagerException
	{
		if (newName.indexOf(this.getFileSeparator()) > -1)
		{
			throw new IllegalArgumentException(Messages.SftpVirtualFileManager_NewNameMustNotBePath);
		}

		boolean result = false;

		ensureConnection();

		if (this._sftpClient != null && file instanceof SftpVirtualFile)
		{
			try
			{
				String beforePath = file.getAbsolutePath();
				String afterPath;

				// update name
				((SftpVirtualFile) file).setName(newName);

				// get full path to new name
				afterPath = file.getAbsolutePath();

				// change name
				this._sftpClient.rename(beforePath, afterPath);

				result = true;
			}
			catch (Exception e)
			{
				// reset name since rename failed
				((SftpVirtualFile) file).setName(newName);

				throw new VirtualFileManagerException(StringUtils.format(
						Messages.SftpVirtualFileManager_UnableToRenameFile, new String[] { file.getAbsolutePath(),
								newName }), e);
			}
		}

		return result;
	}

	/**
	 * permissionStringToLong
	 * 
	 * @param dirEntry
	 * @return long
	 */
	private long permissionStringToLong(String dirEntry)
	{
		// assume read/write
		long permissions = 0666;

		if (dirEntry != null && dirEntry.length() > 0)
		{
			permissions = 0;

			// user
			permissions |= (dirEntry.charAt(1) == 'r') ? (1 << 8) : 0;
			permissions |= (dirEntry.charAt(2) == 'w') ? (1 << 7) : 0;
			permissions |= (dirEntry.charAt(3) == 'x') ? (1 << 6) : 0;

			// group
			permissions |= (dirEntry.charAt(4) == 'r') ? (1 << 5) : 0;
			permissions |= (dirEntry.charAt(5) == 'w') ? (1 << 4) : 0;
			permissions |= (dirEntry.charAt(6) == 'x') ? (1 << 3) : 0;

			// world
			permissions |= (dirEntry.charAt(7) == 'r') ? (1 << 2) : 0;
			permissions |= (dirEntry.charAt(8) == 'w') ? (1 << 1) : 0;
			permissions |= (dirEntry.charAt(9) == 'x') ? (1 << 0) : 0;
		}

		return permissions;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#putStream(java.io.InputStream, 
	 * 			com.aptana.ide.core.io.IVirtualFile)
	 */
	public void putStream(InputStream input, final IVirtualFile targetFile)
			throws ConnectionException, VirtualFileManagerException, IOException
	{
		putStream(input, targetFile, null);
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#putStream(java.io.InputStream, 
	 * 			com.aptana.ide.core.io.IVirtualFile, com.aptana.ide.core.io.IFileProgressMonitor)
	 */
	public synchronized void putStream(InputStream input, final IVirtualFile targetFile, final IFileProgressMonitor monitor)
			throws ConnectionException, VirtualFileManagerException, IOException
	{
		if (input == null)
		{
			throw new IllegalArgumentException(Messages.SftpVirtualFileManager_InputStreamCannotBeNull);
		}

		int tries = 2;

		while (tries > 0)
		{
			ensureConnection();

			if (monitor == null)
			{
				this._sftpClient.setProgressMonitor(null);
			}
			else
			{
				this._sftpClient.setProgressMonitor(new FTPProgressMonitor()
				{
					public void bytesTransferred(long bytes)
					{
						monitor.bytesTransferred(bytes);
					}

				});
			}
			try
			{
				long permissions = targetFile.exists() ? 0 : FilePrefUtils .getFilePermission();
				this._sftpClient.put(input, targetFile.getAbsolutePath());
				// If the file exists, keeps the permissions; otherwise, set the
				// file to the permissions defined in the preferences
				if (permissions > 0)
				{
				    targetFile.setPermissions(permissions);
				}

				tries = 0;
				
				if (monitor != null)
				{
					monitor.done();
				}
			}
			catch (SshException e)
			{
				if ("The SFTP connection has been closed".equals(e.getMessage()) && tries > 1) //$NON-NLS-1$
				{
					// unfortunately we are not getting reliable connect info. In this case ensureConnection
					// thought we were connected, but we weren't, so we force a disconnect and try again
					this.disconnect();

					// Let's try this approach once
					tries--;
				}
				else
				{
					throw new VirtualFileManagerException(e);
				}
			}
			catch (Exception e)
			{
				throw new VirtualFileManagerException(e);
			}
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#getHashString()
	 */
	public String getHashString()
	{
		StringBuffer result = new StringBuffer();

		result.append(this.getNickName() + ISerializableSyncItem.DELIMITER);
		result.append(this.getServer() + ISerializableSyncItem.DELIMITER);
		result.append(this.getBasePath() + ISerializableSyncItem.DELIMITER);
		result.append(this.getUser() + ISerializableSyncItem.DELIMITER);

		if (this.getSavePassword())
		{
			result.append(this.getPassword() + ISerializableSyncItem.DELIMITER);
		}
		else
		{
			result.append(StringUtils.EMPTY + ISerializableSyncItem.DELIMITER);
		}

		result.append(this.getId() + ISerializableSyncItem.DELIMITER);
		result.append(this.isAutoCalculateServerTimeOffset() + ISerializableSyncItem.DELIMITER);

		try
		{
			result.append(this.getTimeOffset() + ISerializableSyncItem.DELIMITER);
		}
		catch (ConnectionException e)
		{
			result.append(0 + ISerializableSyncItem.DELIMITER);
		}

		result.append(this.getPort() + ISerializableSyncItem.DELIMITER);
		result.append(this.serializeCloakedFiles(getCloakedFiles()) + ISerializableSyncItem.DELIMITER);
		result.append(StringUtils.join(ISerializableSyncItem.FILE_DELIMITER, getCloakedFileExpressions())
				+ ISerializableSyncItem.DELIMITER);
		result.append(this.getSavePassword() + ISerializableSyncItem.DELIMITER);
		result.append(this.getPrivateKeyFile() + ISerializableSyncItem.DELIMITER);

		return result.toString();
	}

	/**
	 * @see com.aptana.ide.core.io.sync.ISerializableSyncItem#fromSerializableString(java.lang.String)
	 */
	public void fromSerializableString(String s)
	{
		String[] items = s.split(ISerializableSyncItem.DELIMITER);

		if (items.length >= 6)
		{
			setNickName(items[0]);
			setServer(items[1]);
			if (items[2] == null || "".equals(items[2])) //$NON-NLS-1$
			{
				setBasePath("/"); //$NON-NLS-1$
			}
			else
			{
				setBasePath(items[2]);
			}
			setUser(items[3]);
			setPassword(items[4]);
			String idString = items[5].replace(ISerializableSyncItem.DELIMITER.charAt(0), ' ').trim();
			try
			{
				setId(Long.parseLong(idString));
			}
			catch (NumberFormatException e)
			{
			}
		}
		if (items.length >= 8)
		{
			setAutoCalculateServerTimeOffset(Boolean.valueOf(items[6]).booleanValue());
			setTimeOffset(Long.parseLong(items[7]));
		}
		if (items.length >= 9)
		{
			setPort(Integer.parseInt(items[8]));
		}
		if (items.length >= 10)
		{
			IVirtualFile[] files = deserializeCloakedFiles(items[9]);
			for (int i = 0; i < files.length; i++)
			{
				addCloakedFile(files[i]);
			}
		}
		if (items.length >= 11)
		{
			String[] files = items[10].split(ISerializableSyncItem.FILE_DELIMITER);
			for (int i = 0; i < files.length; i++)
			{
				addCloakExpression(files[i]);
			}
		}
		if (items.length >= 12)
		{
			setSavePassword(Boolean.valueOf(items[11]).booleanValue());
		}
		if (items.length >= 13)
		{
			setPrivateKeyFile(items[12]);
		}
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#containsFile(com.aptana.ide.core.io.IVirtualFile)
	 */
	public boolean containsFile(IVirtualFile file)
	{
		Path otherPath = new Path(file.getAbsolutePath());
		Path thisPath = new Path(this.getBaseFile().getAbsolutePath());

		return thisPath.isPrefixOf(otherPath);
	}

	/**
	 * @see com.aptana.ide.core.io.sync.ISerializableSyncItem#getType()
	 */
	public String getType()
	{
		return this.getClass().getName();
	}

	/**
	 * @see com.aptana.ide.core.io.sync.ISerializableSyncItem#toSerializableString()
	 */
	public String toSerializableString()
	{
		return getHashString();
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#resolveBasePath()
	 */
	public synchronized void resolveBasePath() throws ConnectionException, VirtualFileManagerException
	{
		String fileSeparator = this.getFileSeparator();
		String basePath = (this._basePath != null && this._basePath.length() > 0) ? this._basePath : StringUtils.EMPTY;

		ensureConnection();

		try
		{
			if (this._basePath == null || this._basePath.length() == 0)
			{
				// use the current directory
				this._basePath = this._sftpClient.pwd();
			}
			else
			{
				// default to user value
				this._basePath = basePath;

				// try to go to that directory
				try
				{
					this._sftpClient.chdir(basePath);
				}
				catch (Exception e)
				{
				}

				// use the directory where we ended up
				this._basePath = this._sftpClient.pwd();
			}
		}
		catch (Exception e)
		{
			throw new VirtualFileManagerException(e);
		}

		if (this._basePath.length() > 1 && this._basePath.endsWith(fileSeparator))
		{
			this._basePath = this._basePath.substring(0, this._basePath.length() - 1);
		}
	}

	/**
	 * setModificationMillis
	 * 
	 * @param file
	 * @param modificationTime
	 * @throws IOException
	 * @throws ConnectionException
	 */
	public synchronized void setModificationMillis(SftpVirtualFile file, long modificationTime) throws IOException,
			ConnectionException
	{
		ensureConnection();

		long offset = 0;

		try
		{
			offset = this.getTimeOffset();
		}
		catch (ConnectionException e)
		{
		}

		try
		{
			FTPFile[] files = this._sftpClient.dirDetails(file.getAbsolutePath());

			if (files.length > 0)
			{
				// files[0].setLastModified(new Date(modificationTime + offset));
				this._sftpClient.setModTime(file.getAbsolutePath(), new Date(modificationTime + offset));
			}
		}
		catch (Exception e)
		{
			if (file != null)
			{
				IdeLog.logError(SyncingPlugin.getDefault(), Messages.SftpVirtualFileManager_ERR_setModTime1 + file.getAbsolutePath(), e);
			}
			else
			{
				IdeLog.logError(SyncingPlugin.getDefault(), Messages.SftpVirtualFileManager_ERR_setModTime, e);
			}
		}
	}

	/**
	 * exists
	 * 
	 * @param file
	 * @return boolean
	 * @throws ConnectionException
	 */
	public synchronized boolean exists(SftpVirtualFile file) throws ConnectionException
	{
		ensureConnection();

		boolean result = false;

		try
		{
			result = this._sftpClient.exists(file.getAbsolutePath());
		}
		catch (Exception e)
		{
		}

		return result;
	}

	/**
	 * @see VirtualManagerBase#getPreferenceStore()
	 */
	protected IPreferenceStore getPreferenceStore()
	{
		if (PluginUtils.isPluginLoaded(CoreUIPlugin.getDefault()))
		{
			return CoreUIPlugin.getDefault().getPreferenceStore();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Determines if the file path is valid. For example, '.', '..', and names containing '->' are not considered valid
	 * 
	 * @param name
	 * @return boolean
	 */
	private boolean validName(String name)
	{
		return name.equals(".") == false && name.equals("..") == false; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * setPermissions
	 * 
	 * @param sftpVirtualFile
	 * @param permissions
	 * @return - true if permissions were set
	 */
	public synchronized boolean setPermissions(SftpVirtualFile sftpVirtualFile, long permissions)
	{
		boolean result = true;

		try
		{
			ensureConnection();

			this._sftpClient.changeMode((int) permissions, sftpVirtualFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			result = false;
		}

		return result;
	}
	
	/**
	 * @param descriptiveLabel
	 *            the descriptiveLabel to set
	 */
	public void setDescriptiveLabel(String descriptiveLabel)
	{
		this.descriptiveLabel = descriptiveLabel;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#cloneManager()
	 */
	public IVirtualFileManager cloneManager()
	{
		SftpVirtualFileManager manager = new SftpVirtualFileManager(this.getProtocolManager());
		manager.setId(getId());
		manager.setPassword(this.getPassword());
		manager.setPrivateKeyFile(this.getPrivateKeyFile());
		manager.setUser(this.getUser());
		manager.setAutoCalculateServerTimeOffset(this.isAutoCalculateServerTimeOffset());
		manager.setBasePath(this.getBasePath());
		manager.setCloakedFiles(this.getCloakedFiles());
		manager.setDisabledImage(this.getDisabledImage());
		manager.setHidden(this.isHidden());
		manager.setImage(this.getImage());
		manager.setPort(this.getPort());
		manager.setServer(this.getServer());
		return manager;
	}

	/**
	 * @see com.aptana.ide.core.io.IVirtualFileManager#cancel()
	 */
	public void cancel()
	{
		if (isConnected())
		{
			this._sftpClient.cancelTransfer();
		}
	}

	/**
	 * Returns false for this SFTP connection implementation.
	 */
	public boolean getPassiveMode()
	{
		return false;
	}

	/**
	 * Does not affect this SFTP connection
	 */
	public void setPassiveMode(boolean selection)
	{
	}

	/*
	 * @see com.aptana.ide.io.ftp.IFtpVirtualFileManager#getDialogFactory()
	 */
	public FtpDialogFactory getDialogFactory()
	{
		return SftpDialogFactory.getInstance();
	}
	
	public boolean supportsPublicKeyAuthentication()
	{
		return true;
	}
	
	public String getPrivateKeyFile()
	{
		return this._keyFile;
	}
	
	public void setPrivateKeyFile(String keyFile)
	{
		this._keyFile = keyFile;		
	}

}
