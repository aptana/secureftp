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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.io.ConnectionContext;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.preferences.PreferenceUtils;
import com.aptana.ide.core.io.vfs.ExtendedFileInfo;
import com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager;
import com.aptana.ide.filesystem.ftp.ExpiringMap;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.ssh.SSHFTPAlgorithm;
import com.enterprisedt.net.ftp.ssh.SSHFTPClient;
import com.enterprisedt.net.ftp.ssh.SSHFTPException;
import com.enterprisedt.net.ftp.ssh.SSHFTPInputStream;
import com.enterprisedt.net.ftp.ssh.SSHFTPOutputStream;
import com.enterprisedt.net.j2ssh.configuration.SshConnectionProperties;
import com.enterprisedt.net.j2ssh.transport.publickey.InvalidSshKeyException;
import com.enterprisedt.net.j2ssh.transport.publickey.SshPrivateKeyFile;

/**
 * @author Max Stepanov
 *
 */
/* package */ class SFTPConnectionFileManager extends BaseFTPConnectionFileManager implements ISFTPConnectionFileManager {

	protected static final int SLEEP_INTERVAL = 10; /* 10 secs */

	private SSHFTPClient ftpClient;
	private IPath keyFilePath;
	private String transferType;
	private String controlEncoding;
	private String compression;
	private IPath cwd;
	private Map<IPath, FTPFile> ftpFileCache = new ExpiringMap<IPath, FTPFile>(CACHE_TTL);

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.secureftp.ISFTPConnectionFileManager#init(java.lang.String, int, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, java.lang.String, char[], java.lang.String, java.lang.String, java.lang.String)
	 */
	public void init(String host, int port, IPath basePath, IPath keyFilePath, String login, char[] password, String transferType, String encoding, String compression) {
		Assert.isTrue(ftpClient == null, "SFTP connection has been already initiated");
		try {
			ftpClient = new SSHFTPClient();
			this.host = host;
			this.port = port;
			this.keyFilePath = keyFilePath;
			this.login = login;
			this.password = password;
			this.basePath = basePath != null ? basePath : Path.ROOT;
			if (keyFilePath != null) {
				this.authId = Policy.generateAuthId("SFTP/PUBLICKEY", login, host, port); //$NON-NLS-1$				
			} else {
				this.authId = Policy.generateAuthId("SFTP", login, host, port); //$NON-NLS-1$
			}
			this.transferType = transferType;
			this.controlEncoding = encoding;
			this.compression = compression;
			initFTPClient(ftpClient, encoding, compression);
		} catch (Exception e) {
			IdeLog.logImportant(SecureFTPPlugin.getDefault(), "SFTP connection initialization failed", e);
			ftpClient = null;
		}		
	}

	private static void initFTPClient(SSHFTPClient ftpClient, String encoding, String compression) throws IOException, FTPException {
		ftpClient.setTimeout(TIMEOUT);
		ftpClient.setControlEncoding(encoding);
		ftpClient.setMonitorInterval(1024);
		ftpClient.setSleepEnabled(true);
		ftpClient.setSleepTime(SLEEP_INTERVAL);
		if (ISFTPConstants.COMPRESSION_NONE.equals(compression)) {
			ftpClient.disableAllAlgorithms(SSHFTPAlgorithm.COMPRESSION);
			ftpClient.setAlgorithmEnabled(SSHFTPAlgorithm.COMPRESSION_NONE, true);
		} else if (ISFTPConstants.COMPRESSION_ZLIB.equals(compression)) {
			ftpClient.disableAllAlgorithms(SSHFTPAlgorithm.COMPRESSION);
			ftpClient.setAlgorithmEnabled(SSHFTPAlgorithm.COMPRESSION_ZLIB, true);			
		}
		ftpClient.setTransportProvider(SshConnectionProperties.USE_STANDARD_SOCKET);
		ftpClient.setConfigFlags(0);
		ftpClient.setTransferBufferSize(TRANSFER_BUFFER_SIZE);
		ftpClient.setValidator(new SSHHostValidator());
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#connect(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void connect(IProgressMonitor monitor) throws CoreException {
		Assert.isTrue(ftpClient != null, "SFTP connection is not initialized");
		monitor = Policy.monitorFor(monitor);
		try {
			if (ftpClient.connected()) {
				monitor.beginTask("Checking connection", IProgressMonitor.UNKNOWN);
				ftpClient.pwd();
				return;
			}
			cwd = null;
			cleanup();

			ConnectionContext context = CoreIOPlugin.getConnectionContext(this);
			
			monitor.beginTask("Establishing connection", IProgressMonitor.UNKNOWN);
			ftpClient.setRemoteHost(host);
			ftpClient.setRemotePort(port);
			while (true) {
				if (keyFilePath != null) {
					SshPrivateKeyFile privateKeyFile;
					try {
						privateKeyFile = SshPrivateKeyFile.parse(keyFilePath.toFile());
					} catch (InvalidSshKeyException e) {
						throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Private Key file {0} is invalid.", keyFilePath.toOSString()), e));
					} catch (IOException e) {
						throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Private Key file {0} cannot be read.", keyFilePath.toOSString())));
					}
					if (privateKeyFile.isPassphraseProtected() && (password == null || password.length == 0)) {
						if (context != null && context.getBoolean(ConnectionContext.NO_PASSWORD_PROMPT)) {
							password = new char[0];
						} else {
							getOrPromptPassword(StringUtils.format("Public Key Authentication for {0} using {1}", new Object[] { host, keyFilePath.toOSString() }), "Please specify private key passphrase.");
							while (true) {
								try {
									privateKeyFile.toPrivateKey(String.copyValueOf(password));
								} catch (InvalidSshKeyException e) {
									promptPassword(StringUtils.format("Public Key Authentication for {0} using {1}", new Object[] { host, keyFilePath.toOSString() }), "Passphrase was not accepted. Please specify again.");
									continue;
								}
								break;
							}
						}
					}
					try {
						ftpClient.setAuthentication(keyFilePath.toOSString(), login, String.copyValueOf(password));
					} catch (InvalidSshKeyException e) {
						if (password == null || password.length == 0) {
							throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Private Key {0} requires passphrase.", keyFilePath.toOSString()), e));													
						}
						throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Passphrase for Private Key {0} is invalid.", keyFilePath.toOSString()), e));						
					}
				} else {
					if (password == null) {
						if (ISFTPConstants.LOGIN_ANONYMOUS.equals(login)) {
							password = new char[0];
						} else if (context != null && context.getBoolean(ConnectionContext.NO_PASSWORD_PROMPT)) {
							password = new char[0];
						} else {
							getOrPromptPassword(StringUtils.format("SFTP Authentication for {0}", host), "Please specify password.");
						}
					}
					ftpClient.setAuthentication(login, String.copyValueOf(password));
				}
				Policy.checkCanceled(monitor);
				monitor.subTask("connecting and authenticating");
				try {
					ftpClient.connect();
				} catch (SSHFTPException e) {
					Policy.checkCanceled(monitor);
					if (keyFilePath != null) {
						throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Public Key Authentication failed: {0}", e.getLocalizedMessage()), e));
					}
					if (context != null && context.getBoolean(ConnectionContext.NO_PASSWORD_PROMPT)) {
						throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Authentication failed: {0}", "Login/password is incorrect."), e));
					}
					promptPassword(StringUtils.format("SFTP Authentication for {0}", host), "Password was not accepted. Please specify again.");
					safeQuit();
					continue;
				}
				break;
			}
			
			Policy.checkCanceled(monitor);
			changeCurrentDir(basePath);

			ftpClient.setType(ISFTPConstants.TRANSFER_TYPE_ASCII.equals(transferType)
					? FTPTransferType.ASCII : FTPTransferType.BINARY);
			
		} catch (OperationCanceledException e) {
			safeQuit();
			throw e;
		} catch (CoreException e) {
			safeQuit();
			throw e;
		} catch (UnknownHostException e) {
			safeQuit();
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Host name not found: "+e.getLocalizedMessage(), e));
		} catch (FileNotFoundException e) {
			safeQuit();
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Remote folder not found: "+e.getLocalizedMessage(), e));			
		} catch (Exception e) {
			safeQuit();
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Establishing SFTP connection failed:"+e.getLocalizedMessage(), e));
		} finally {
			monitor.done();
		}
	}

	private void safeQuit() {
		try {
			if (ftpClient.connected()) {
				ftpClient.quit();
			}
		} catch (Exception e) {
			try {
				ftpClient.quitImmediately();
			} catch (Exception ignore) {
			}
		}		
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#disconnect(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void disconnect(IProgressMonitor monitor) throws CoreException {
		try {
			checkConnected();
		} catch (Exception ignore) {
		}
		if (!isConnected()) {
			return;
		}
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask("Closing SFTP connection", IProgressMonitor.UNKNOWN);
		try {
			ftpClient.quit();
		} catch (Exception e) {
			try {
				ftpClient.quitImmediately();
			} catch (Exception ignore) {
			}
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Disconnect SFTP connection failed", e));
		} finally {
			cwd = null;
			cleanup();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#isConnected()
	 */
	public boolean isConnected() {
		return ftpClient != null && ftpClient.connected();
	}

	private void changeCurrentDir(IPath path) throws FTPException, IOException {
		try {
			if (cwd == null) {
				cwd = new Path(ftpClient.pwd());
			}
			if (!cwd.equals(path)) {
				ftpClient.chdir(path.toPortableString());
				cwd = path;
			}
		} catch (FTPException e) {
			throwFileNotFound(e, path);
		} catch (IOException e) {
			cwd = null;
			throw e;			
		}
	}

	private static void throwFileNotFound(FTPException e, IPath path) throws FileNotFoundException, FTPException {
		if (e.getReplyCode() == -1) {
			throw new FileNotFoundException(path.toPortableString());
		}
		throw e;		
	}

	private static void fillFileInfo(ExtendedFileInfo fileInfo, FTPFile ftpFile) {
		fileInfo.setExists(true);
		fileInfo.setName(ftpFile.getName());
		fileInfo.setDirectory(ftpFile.isDir());
		fileInfo.setLength(ftpFile.size());
		fileInfo.setLastModified(ftpFile.lastModified() != null ? ftpFile.lastModified().getTime() : 0);
		fileInfo.setOwner(ftpFile.getOwner());
		fileInfo.setGroup(ftpFile.getGroup());
		fileInfo.setPermissions(Policy.permissionsFromString(ftpFile.getPermissions()));
		if (ftpFile.isLink()) {
			fileInfo.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
			fileInfo.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, ftpFile.getLinkedName().trim());
		}
	}
	
	private static ExtendedFileInfo createFileInfo(FTPFile ftpFile) {
		ExtendedFileInfo fileInfo = new ExtendedFileInfo(ftpFile.getName());
		fillFileInfo(fileInfo, ftpFile);
		return fileInfo;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#clearCache(org.eclipse.core.runtime.IPath)
	 */
	@Override
	protected void clearCache(IPath path) {
		super.clearCache(path);
		path = basePath.append(path); // we cache as absolute paths
		int segments = path.segmentCount();
		for (IPath p : new ArrayList<IPath>(ftpFileCache.keySet())) {
			if (p.segmentCount() >= segments && path.matchingFirstSegments(p) == segments) {
				ftpFileCache.remove(p);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#checkConnected()
	 */
	@Override
	protected void checkConnected() throws Exception {
		if (ftpClient.connected()) {
			try {
				ftpClient.keepAlive();
				return;
			} catch (FTPException ignore) {
				return;
			} catch (IOException e) {
			}
			ftpClient.quitImmediately();
		}
	}


	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#getRootCanonicalURI()
	 */
	@Override
	protected URI getRootCanonicalURI() {
		try {
			return new URI("sftp", login, host, port != ISFTPConstants.SFTP_PORT_DEFAULT ? port : -1, Path.ROOT.toPortableString(), null, null); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#fetchFile(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected ExtendedFileInfo fetchFile(IPath path, int options, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			IPath dirPath = path.removeLastSegments(1);
			String name = path.lastSegment();
			FTPFile result = ftpFileCache.get(path);
			if (result == null) {
				FTPFile[] ftpFiles = listFiles(dirPath, monitor);
				for (FTPFile ftpFile : ftpFiles) {
					if (".".equals(ftpFile.getName()) || "..".equals(ftpFile.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
						if (Path.ROOT.equals(path) && ".".equals(ftpFile.getName())) { //$NON-NLS-1$
							ftpFile.setName(path.toPortableString());
							ftpFileCache.put(path, ftpFile);
							result = ftpFile;
						}
						continue;
					}
					ftpFileCache.put(dirPath.append(ftpFile.getName()), ftpFile);
					if (name != null && name.equals(ftpFile.getName())) {
						result = ftpFile;
					}
				}
			}
			if (result != null) {
				return createFileInfo(result);
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Fetching file info failed", e));			
		}
		ExtendedFileInfo fileInfo = new ExtendedFileInfo(path.lastSegment());
		fileInfo.setExists(false);
		return fileInfo;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#fetchFiles(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected ExtendedFileInfo[] fetchFiles(IPath path, int options, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		monitor = Policy.subMonitorFor(monitor, 1);
		try {
			FTPFile[] ftpFiles = listFiles(path, monitor);
			monitor.beginTask("Gethering files details", ftpFiles.length);
			List<ExtendedFileInfo> list = new ArrayList<ExtendedFileInfo>();
			for (FTPFile ftpFile : ftpFiles) {
				if (".".equals(ftpFile.getName()) || "..".equals(ftpFile.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
					monitor.worked(1);
					continue;
				}
				IPath filePath = path.append(ftpFile.getName());
				ftpFileCache.put(filePath, ftpFile);
				
				ExtendedFileInfo fileInfo = createFileInfo(ftpFile);
				list.add(fileInfo);
				monitor.worked(1);
			}
			return list.toArray(new ExtendedFileInfo[list.size()]);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Fetching directory failed", e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#createDirectory(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void createDirectory(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			try {
				try {
					changeCurrentDir(path);
					return; // directory exists - return
				} catch (FileNotFoundException ignore) {
				}
				ftpClient.mkdir(path.toPortableString());
				changeFilePermissions(path, PreferenceUtils.getDirectoryPermissions(), monitor);
			} catch (FTPException e) {
				throwFileNotFound(e, path);
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Creating directory failed", e));			
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#deleteDirectory(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void deleteDirectory(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		MultiStatus status = new MultiStatus(SecureFTPPlugin.PLUGIN_ID, 0, null, null);
		try {
			IPath dirPath = path.removeLastSegments(1);
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			recursiveDeleteTree(path, monitor, status);
			changeCurrentDir(dirPath);
			ftpClient.rmdir(path.lastSegment());
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			if (!status.isOK()) {
				MultiStatus multiStatus = new MultiStatus(SecureFTPPlugin.PLUGIN_ID, 0, "Deleting directory failed", e);
				multiStatus.addAll(status);
			} else {
				throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Deleting directory failed", e));
			}
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#deleteFile(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void deleteFile(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			IPath dirPath = path.removeLastSegments(1);
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			try {
				ftpClient.delete(path.lastSegment());
			} catch (FTPException e) {
			    IdeLog.logError(SecureFTPPlugin.getDefault(), "Failed to delete " + path.toString(), e);
				throw e;
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID,
					StringUtils.format("Deleting {0} failed", path), e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#renameFile(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void renameFile(IPath sourcePath, IPath destinationPath, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			changeCurrentDir(Path.ROOT);
			Policy.checkCanceled(monitor);
			if (ftpClient.exists(destinationPath.toPortableString())) {
				ftpClient.delete(destinationPath.toPortableString());
			}
			try {
				ftpClient.rename(sourcePath.toPortableString(), destinationPath.toPortableString());
			} catch (FTPException e) {
				throwFileNotFound(e, sourcePath);
                IdeLog.logError(SecureFTPPlugin.getDefault(), StringUtils
                        .format("Failed to rename {0} to {1}", new Object[] { sourcePath,
                                destinationPath }), e);
				throw e;
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Renaming failed", e));			
		} finally {
			monitor.done();
		}
	}


	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#listDirectory(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected String[] listDirectory(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			FTPFile[] ftpFiles = listFiles(path, monitor);
			List<String> list = new ArrayList<String>();
			for (FTPFile ftpFile : ftpFiles) {
				String name = ftpFile.getName();
				if (".".equals(name) || "..".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
				ftpFileCache.put(path.append(ftpFile.getName()), ftpFile);
				list.add(name);
			}
			return list.toArray(new String[list.size()]);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Listing directory failed", e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#readFile(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected InputStream readFile(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		monitor.beginTask("Initiating file download", 4);
		SSHFTPClient downloadFtpClient = new SSHFTPClient();
		try {
			initFTPClient(downloadFtpClient, controlEncoding, compression);
			downloadFtpClient.setValidator(ftpClient.getValidator());
			downloadFtpClient.setRemoteHost(host);
			downloadFtpClient.setRemotePort(port);
			Policy.checkCanceled(monitor);
			downloadFtpClient.setAuthentication(login, String.copyValueOf(password));
			/* TODO: key auth */
			downloadFtpClient.connect();
			monitor.worked(1);
			Policy.checkCanceled(monitor);
			downloadFtpClient.setType(ISFTPConstants.TRANSFER_TYPE_ASCII.equals(transferType)
					? FTPTransferType.ASCII : FTPTransferType.BINARY);
			try {
				downloadFtpClient.chdir(path.removeLastSegments(1).toPortableString());
			} catch (FTPException e) {
				throwFileNotFound(e, path.removeLastSegments(1));
			}
			monitor.worked(1);
			Policy.checkCanceled(monitor);
			try {
				return new FTPFileDownloadInputStream(downloadFtpClient,
						new SSHFTPInputStream(downloadFtpClient, path.lastSegment()));
			} catch (FTPException e) {
				throwFileNotFound(e, path);
				return null;
			}
		} catch (Exception e) {
			if (downloadFtpClient.connected()) {
				try {
					if (e instanceof OperationCanceledException
							|| e instanceof FTPException
							|| e instanceof FileNotFoundException) {
						downloadFtpClient.quit();
					} else {
						downloadFtpClient.quitImmediately();
					}
				} catch (IOException ignore) {
				} catch (FTPException ignore) {
				}
			}
			if (e instanceof OperationCanceledException) {
				throw (OperationCanceledException) e;
			} else if (e instanceof FileNotFoundException) {
				throw (FileNotFoundException) e;
			}
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Opening file failed", e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#writeFile(org.eclipse.core.runtime.IPath, long, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected OutputStream writeFile(IPath path, long permissions, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		monitor.beginTask("Initiating file upload", 4);
		SSHFTPClient uploadFtpClient = new SSHFTPClient();
		try {
			initFTPClient(uploadFtpClient, controlEncoding, compression);
			uploadFtpClient.setValidator(ftpClient.getValidator());
			uploadFtpClient.setRemoteHost(host);
			uploadFtpClient.setRemotePort(port);
			uploadFtpClient.setAuthentication(login, String.copyValueOf(password));
			Policy.checkCanceled(monitor);
			uploadFtpClient.connect();
			monitor.worked(1);
			Policy.checkCanceled(monitor);
			uploadFtpClient.setType(ISFTPConstants.TRANSFER_TYPE_ASCII.equals(transferType)
					? FTPTransferType.ASCII : FTPTransferType.BINARY);
			IPath dirPath = path.removeLastSegments(1);
			try {
				uploadFtpClient.chdir(dirPath.toPortableString());
			} catch (FTPException e) {
				throwFileNotFound(e, dirPath);
			}
			monitor.worked(1);
			Policy.checkCanceled(monitor);
			return new FTPFileUploadOutputStream(uploadFtpClient,
					new SSHFTPOutputStream(uploadFtpClient, generateTempFileName(path.lastSegment())),
					path.lastSegment(),
					new Date(), permissions);
		} catch (Exception e) {
			if (uploadFtpClient.connected()) {
				try {
					if (e instanceof OperationCanceledException
							|| e instanceof FTPException
							|| e instanceof FileNotFoundException) {
						uploadFtpClient.quit();
					} else {
						uploadFtpClient.quitImmediately();
					}
				} catch (IOException ignore) {
				} catch (FTPException ignore) {
				}
			}
			if (e instanceof OperationCanceledException) {
				throw (OperationCanceledException) e;
			} else if (e instanceof FileNotFoundException) {
				throw (FileNotFoundException) e;
			}
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Opening file failed", e));			
		} finally {
			monitor.done();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#setModificationTime(org.eclipse.core.runtime.IPath, long, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void setModificationTime(IPath path, long modificationTime, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			IPath dirPath = path.removeLastSegments(1);
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			ftpClient.setModTime(path.lastSegment(), new Date(modificationTime));
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Set modification time failed", e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#changeFileGroup(org.eclipse.core.runtime.IPath, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void changeFileGroup(IPath path, String group, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			IPath dirPath = path.removeLastSegments(1);
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			try {
				int gid = Integer.parseInt(group);
				ftpClient.changeGroup(gid, path.lastSegment());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Group is not numeric");
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Set permissions failed", e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager#changeFilePermissions(org.eclipse.core.runtime.IPath, long, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void changeFilePermissions(IPath path, long permissions, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			IPath dirPath = path.removeLastSegments(1);
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			ftpClient.changeMode((int) (permissions & 0777), path.lastSegment());
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Set permissions failed", e));			
		} finally {
			monitor.done();
		}
	}

	private FTPFile[] listFiles(IPath dirPath, IProgressMonitor monitor) throws IOException, ParseException, FTPException {
		changeCurrentDir(dirPath);
		Policy.checkCanceled(monitor);
		return ftpClient.dirDetails("."); //$NON-NLS-1$
	}

	private void recursiveDeleteTree(IPath path, IProgressMonitor monitor, MultiStatus status) throws IOException, ParseException {
		try {
			changeCurrentDir(path);
			FTPFile[] ftpFiles = listFiles(path, monitor);
			List<String> dirs = new ArrayList<String>();
			for (FTPFile ftpFile: ftpFiles) {
				String name = ftpFile.getName();
				if (".".equals(name) || "..".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
				if (ftpFile.isDir()) {
					dirs.add(name);
					continue;
				}
				Policy.checkCanceled(monitor);
				monitor.subTask(path.append(name).toPortableString());
				try {
					ftpClient.delete(name);
				} catch (FTPException e) {
					status.add(new Status(IStatus.ERROR, SecureFTPPlugin.PLUGIN_ID,
							StringUtils.format("Deleting {0} failed", path.append(name).toPortableString()), e));
				}
				monitor.worked(1);
			}
			for (String name : dirs) {
				monitor.subTask(path.append(name).toPortableString());
				recursiveDeleteTree(path.append(name), monitor, status);
				Policy.checkCanceled(monitor);
				changeCurrentDir(path);
				Policy.checkCanceled(monitor);
				ftpClient.rmdir(name);
				monitor.worked(1);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			status.add(new Status(IStatus.ERROR, SecureFTPPlugin.PLUGIN_ID,
					StringUtils.format("Deleting {0} failed", path.toPortableString()), e));
		}
	}

	private static String generateTempFileName(String base) {
		StringBuffer sb = new StringBuffer();
		sb.append(TMP_UPLOAD_PREFIX).append(base);
		return sb.toString();
	}

}
