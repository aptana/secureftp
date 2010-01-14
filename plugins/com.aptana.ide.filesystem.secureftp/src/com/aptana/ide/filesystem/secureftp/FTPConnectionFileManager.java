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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;

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
import com.aptana.ide.core.io.vfs.IExtendedFileStore;
import com.aptana.ide.filesystem.ftp.ExpiringMap;
import com.aptana.ide.filesystem.ftp.FTPPlugin;
import com.aptana.ide.filesystem.ftp.IFTPConnectionFileManager;
import com.aptana.ide.filesystem.ftp.BaseFTPConnectionFileManager;
import com.aptana.ide.filesystem.ftp.IFTPConstants;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPConnectionClosedException;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPFileFactory;
import com.enterprisedt.net.ftp.FTPInputStream;
import com.enterprisedt.net.ftp.FTPMessageListener;
import com.enterprisedt.net.ftp.FTPOutputStream;
import com.enterprisedt.net.ftp.FTPReply;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.pro.ProFTPClient;

/**
 * @author Max Stepanov
 *
 */
/* package */ class FTPConnectionFileManager extends BaseFTPConnectionFileManager implements IFTPConnectionFileManager {
	
	private static final String TMP_TIMEZONE_CHECK = ".tmp_tz_check"; //$NON-NLS-1$
	
	protected FTPClient ftpClient;
	private List<String> serverFeatures;
	protected String transferType;
	protected String timezone;
	protected IPath cwd;
	private FTPFileFactory fileFactory;
	private Boolean statSuppoted = null;
	private Boolean chmodSupported = null;
	private Boolean chgrpSupported = null;
	private Map<IPath, FTPFile> ftpFileCache = new ExpiringMap<IPath, FTPFile>(CACHE_TTL);
	private long serverTimeZoneShift = Integer.MIN_VALUE;
	protected boolean hasServerInfo;
	protected PrintWriter messageLogWriter;
	
	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.IFTPConnectionFileManager#init(java.lang.String, int, org.eclipse.core.runtime.IPath, java.lang.String, char[], boolean, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void init(String host, int port, IPath basePath, String login, char[] password, boolean passive, String transferType, String encoding, String timezone) {
		Assert.isTrue(ftpClient == null, Messages.FTPConnectionFileManager_already_initialized);
		try {
			ftpClient = createFTPClient();
			this.host = host;
			this.port = port;
			this.login = login;
			this.password = password;
			this.basePath = basePath != null ? basePath : Path.ROOT;
			this.authId = Policy.generateAuthId("FTP", login, host, port); //$NON-NLS-1$
			this.transferType = transferType;
			this.timezone = timezone != null && timezone.length() == 0 ? null : timezone;
			initFTPClient(ftpClient, passive, encoding);
		} catch (Exception e) {
			IdeLog.logImportant(SecureFTPPlugin.getDefault(), Messages.FTPConnectionFileManager_initialization_failed, e);
			ftpClient = null;
		}
	}

	protected FTPClient createFTPClient() {
		return new ProFTPClient();
	}

	protected static void initFTPClient(FTPClient ftpClient, boolean passive, String encoding) throws IOException, FTPException {
		ftpClient.setTimeout(TIMEOUT);
		ftpClient.setControlEncoding(encoding);
		ftpClient.setMonitorInterval(1024);
		ftpClient.setConnectMode(passive ? FTPConnectMode.PASV : FTPConnectMode.ACTIVE);
		ftpClient.setRetryCount(RETRY);
		ftpClient.setRetryDelay(RETRY_DELAY);
		ftpClient.setServerWakeupInterval(KEEPALIVE_INTERVAL);
		ftpClient.setDeleteOnFailure(true);
		ftpClient.setTransferBufferSize(TRANSFER_BUFFER_SIZE);
	}

	protected void initAndAuthFTPClient(FTPClient newFtpClient, IProgressMonitor monitor) throws IOException, FTPException {
		initFTPClient(newFtpClient, ftpClient.getConnectMode() == FTPConnectMode.PASV, ftpClient.getControlEncoding());
		newFtpClient.setRemoteHost(host);
		newFtpClient.setRemotePort(port);
		Policy.checkCanceled(monitor);
		newFtpClient.connect();
		monitor.worked(1);
		Policy.checkCanceled(monitor);
		newFtpClient.login(login, String.copyValueOf(password));
		monitor.worked(1);
	}

	protected static void setMessageLogger(FTPClient ftpClient, final PrintWriter writer) {
		FTPMessageListener listener = null;
		if (writer != null && ftpClient.getMessageListener() == null) {
			listener = new FTPMessageListener() {
				public void logCommand(String command) {
					if (command.startsWith("---> ")) {
						command = command.substring(5);
					}
					Matcher matcher = PASS_COMMAND_PATTERN.matcher(command);
					if (matcher.matches()) {
						command = matcher.replaceFirst("$1********");
					}
					writer.print("ftp> ");
					writer.println(command);
					writer.flush();
				}

				public void logReply(String reply) {
					writer.println(reply);
					writer.flush();
				}
			};
		}
		ftpClient.setMessageListener(listener);

	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#connect(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void connect(IProgressMonitor monitor) throws CoreException {
		Assert.isTrue(ftpClient != null, Messages.FTPConnectionFileManager_not_initialized);
		monitor = Policy.monitorFor(monitor);
		try {
			if (ftpClient.connected()) {
				monitor.beginTask(Messages.FTPConnectionFileManager_checking_connection, IProgressMonitor.UNKNOWN);
				ftpClient.pwd();
				return;
			}
			cwd = null;
			cleanup();

			ConnectionContext context = CoreIOPlugin.getConnectionContext(this);
			
			if (messageLogWriter == null) {
				if (context != null) {
					Object object = context.get(ConnectionContext.COMMAND_LOG);
					if (object instanceof PrintWriter) {
						messageLogWriter = (PrintWriter) object;
					} else if (object instanceof OutputStream) {
						messageLogWriter = new PrintWriter((OutputStream) object);
					}
				}
				if (messageLogWriter == null) {
					messageLogWriter = FTPPlugin.getDefault().getFTPLogWriter();
				}
				if (messageLogWriter != null) {
					messageLogWriter.println(StringUtils.format("---------- FTP {0} ----------", host));
					setMessageLogger(ftpClient, messageLogWriter);
				}
			} else {
				messageLogWriter.println(StringUtils.format("---------- RECONNECTING - FTP {0} ----------", host));
			}

			monitor.beginTask(Messages.FTPConnectionFileManager_establishing_connection, IProgressMonitor.UNKNOWN);
			ftpClient.setRemoteHost(host);
			ftpClient.setRemotePort(port);
			while (true) {
				monitor.subTask(Messages.FTPConnectionFileManager_connecting);
				ftpClient.connect();
				if (password == null) {
					if (IFTPConstants.LOGIN_ANONYMOUS.equals(login)) {
						password = new char[0];
					} else if (context != null && context.getBoolean(ConnectionContext.NO_PASSWORD_PROMPT)) {
						password = new char[0];
					} else {
						getOrPromptPassword(StringUtils.format(Messages.FTPConnectionFileManager_ftp_auth, host), Messages.FTPConnectionFileManager_specify_password);
					}
				}
				Policy.checkCanceled(monitor);
				monitor.subTask(Messages.FTPConnectionFileManager_authenticating);
				try {
					ftpClient.login(login, String.copyValueOf(password));
				} catch (FTPException e) {
					Policy.checkCanceled(monitor);
					if ("331".equals(ftpClient.getLastValidReply().getReplyCode())) { //$NON-NLS-1$
						if (context != null && context.getBoolean(ConnectionContext.NO_PASSWORD_PROMPT)) {
							throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, StringUtils.format("Authentication failed: {0}", e.getLocalizedMessage()), e));
						}
						promptPassword(StringUtils.format(Messages.FTPConnectionFileManager_ftp_auth, host), Messages.FTPConnectionFileManager_invalid_password);
						safeQuit();
						continue;
					}
					throw e;
				}
				break;
			}
			
			Policy.checkCanceled(monitor);
			changeCurrentDir(basePath);

			ftpClient.setType(IFTPConstants.TRANSFER_TYPE_ASCII.equals(transferType)
					? FTPTransferType.ASCII : FTPTransferType.BINARY);

			if ((hasServerInfo || (context != null && context.getBoolean(ConnectionContext.QUICK_CONNECT)))
					&& !(context != null && context.getBoolean(ConnectionContext.DETECT_TIMEZONE))) {
				return;
			}
			getherServerInfo(context, monitor);
			
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
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_connection_failed+e.getLocalizedMessage(), e));
		} finally {
			monitor.done();
		}
	}
	
	@SuppressWarnings("deprecation")
	protected void getherServerInfo(ConnectionContext context, IProgressMonitor monitor) {
		Policy.checkCanceled(monitor);
		monitor.subTask(Messages.FTPConnectionFileManager_gethering_server_info);
		try {
			serverFeatures = null;
			String[] features = ftpClient.features();
			if (features != null && features.length > 0) {
				serverFeatures = new ArrayList<String>();
				for (int i = 0; i < features.length; ++i) {
					String feature = features[i].trim();
					if (feature.indexOf(' ') > 0) {
						feature = feature.substring(0, feature.indexOf(' '));
					}
					serverFeatures.add(feature);
				}
			}
		} catch (Exception e) {
		}
		
		Policy.checkCanceled(monitor);
		FTPFile[] rootFiles = null;
		try {
			rootFiles = ftpSTAT(Path.ROOT.toPortableString());
		} catch (Exception e) {
		}
		statSuppoted = (rootFiles != null && rootFiles.length > 0) ? Boolean.TRUE : Boolean.FALSE;

		if (context != null && context.getBoolean(ConnectionContext.DETECT_TIMEZONE))
		{
			serverTimeZoneShift = Integer.MIN_VALUE;
		} else if (timezone != null) {
			TimeZone tz = TimeZone.getTimeZone(timezone);
			if (tz != null) {
				long time = new Date().getTime();
				serverTimeZoneShift = TimeZone.getDefault().getOffset(time) - tz.getOffset(time);
			}
		}
		if (serverTimeZoneShift == Integer.MIN_VALUE) {
			Policy.checkCanceled(monitor);
			try {
				changeCurrentDir(Path.ROOT);
				FTPFile file = null;
				if (rootFiles != null) {
					for (FTPFile ftpFile : rootFiles) {
						if (ftpFile.isFile()
								&& !ftpFile.getName().startsWith(".ht")
								&& !(ftpFile.lastModified().getHours() == 0
										&& ftpFile.lastModified().getMinutes() == 0
										&& ftpFile.lastModified().getSeconds() == 0)) {
							file = ftpFile;
							break;
						}
					}
				}
				if (file == null && !Path.ROOT.equals(basePath)) {
					FTPFile[] ftpFiles = listFiles(basePath, monitor);
					for (FTPFile ftpFile : ftpFiles) {
						if (ftpFile.isFile()
								&& !ftpFile.getName().startsWith(".ht")
								&& !(ftpFile.lastModified().getHours() == 0
										&& ftpFile.lastModified().getMinutes() == 0
										&& ftpFile.lastModified().getSeconds() == 0)) {
							file = ftpFile;
							break;
						}
					}
				}
				Date lastModifiedLocal = null;
				if (file == null) {
					changeCurrentDir(basePath);
					lastModifiedLocal = new Date();
					ftpClient.put(new ByteArrayInputStream(new byte[] {}), TMP_TIMEZONE_CHECK);
					for (FTPFile ftpFile : listFiles(basePath, monitor)) {
						if (TMP_TIMEZONE_CHECK.equals(ftpFile.getName())) {
							file = ftpFile;
							break;
						}
					}						
				}
				Date lastModifiedServerInLocalTZ = file.lastModified();
				if (file != null) {
					if (serverSupportsFeature("MDTM")) { //$NON-NLS-1$
						Date lastModifiedLocalTZ = ftpClient.modtime(file.getName());
						if (lastModifiedLocalTZ != null) {
							// align to minutes
							serverTimeZoneShift = (lastModifiedLocalTZ.getTime() - lastModifiedLocalTZ.getTime() % 60000) - (lastModifiedServerInLocalTZ.getTime() - lastModifiedServerInLocalTZ.getTime() % 60000);
						}
					}
					if (serverTimeZoneShift == Integer.MIN_VALUE) {
						serverTimeZoneShift = (lastModifiedLocal.getTime() - lastModifiedLocal.getTime() % 60000) - (lastModifiedServerInLocalTZ.getTime() - lastModifiedServerInLocalTZ.getTime() % 60000);
						// align to 1/4 hour
						long rem = serverTimeZoneShift % 900000;
						if (rem < 450000) {
							serverTimeZoneShift -= rem;
						} else {
							serverTimeZoneShift += (900000-rem);
						}
					}
					if (TMP_TIMEZONE_CHECK.equals(file.getName())) {
						ftpClient.delete(file.getName());
					}
					if (context != null) {
						Calendar cal = Calendar.getInstance();
						int rawOffset = (int) (cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET) - serverTimeZoneShift);
						context.put(ConnectionContext.SERVER_TIMEZONE, TimeZone.getAvailableIDs(rawOffset));
					}
				} 
			} catch (OperationCanceledException e) {
				throw e;
			} catch (Exception e) {
				IdeLog.logImportant(SecureFTPPlugin.getDefault(), Messages.FTPConnectionFileManager_server_tz_check, e);
			}
			if (serverTimeZoneShift == Integer.MIN_VALUE) {
				Calendar cal = Calendar.getInstance();
				serverTimeZoneShift = cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET);					
			}
		}
					
		hasServerInfo = true;
		
	}
	
	protected void safeQuit() {
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
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#isConnected()
	 */
	public boolean isConnected() {
		return ftpClient != null && ftpClient.connected();
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#disconnect(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized void disconnect(IProgressMonitor monitor) throws CoreException {
		try {
			checkConnected();
		} catch (Exception ignore) {
		}
		if (!isConnected()) {
			return;
		}
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(Messages.FTPConnectionFileManager_closing_connection, IProgressMonitor.UNKNOWN);
		try {
			ftpClient.quit();
		} catch (Exception e) {
			try {
				ftpClient.quitImmediately();
			} catch (Exception ignore) {
			}
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_disconnect_failed, e));
		} finally {
			cwd = null;
			cleanup();
			monitor.done();
		}
	}

	private boolean serverSupportsFeature(String feature) {
		if (serverFeatures != null) {
			return serverFeatures.contains(feature);
		}
		return false; // assume doesn't supports be default
	}
	
	protected void changeCurrentDir(IPath path) throws FTPException, IOException {
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
		int code = e.getReplyCode();
		if (code == 550 || code == 450) {
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
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#clearCache(org.eclipse.core.runtime.IPath)
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
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#checkConnected()
	 */
	@Override
	protected void checkConnected() throws Exception {
		if (ftpClient.connected()) {
			try {
				ftpClient.noOperation();
				return;
			} catch (FTPConnectionClosedException e) {
			} catch (FTPException ignore) {
				return;
			} catch (IOException e) {
			}
			ftpClient.quitImmediately();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#getRootCanonicalURI()
	 */
	@Override
	protected URI getRootCanonicalURI() {
		try {
			return new URI("ftp", login, host, port != IFTPConstants.FTP_PORT_DEFAULT ? port : -1, Path.ROOT.toPortableString(), null, null); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#fetchFile(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected ExtendedFileInfo fetchFile(IPath path, int options, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			IPath dirPath = path.removeLastSegments(1);
			String name = path.lastSegment();
			FTPFile result = ftpFileCache.get(path);
			if (result == null) {
				FTPFile[] ftpFiles = listFiles(dirPath, monitor);
				for (FTPFile ftpFile : ftpFiles) {
					Date lastModifiedServerInLocalTZ = ftpFile.lastModified();
					if (serverTimeZoneShift != 0) {
						ftpFile.setLastModified(new Date(lastModifiedServerInLocalTZ.getTime()+serverTimeZoneShift));
					}
					if (".".equals(ftpFile.getName()) || "..".equals(ftpFile.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
						if (Path.ROOT.equals(path) && ".".equals(ftpFile.getName())) { //$NON-NLS-1$
							ftpFile.setName(path.toPortableString());
							ftpFileCache.put(path, ftpFile);
							result = ftpFile;
						}
						continue;
					}
					ftpFileCache.put(dirPath.append(ftpFile.getName()), ftpFile);
					if (name != null && name.equalsIgnoreCase(ftpFile.getName())) {
						result = ftpFile;
					}
				}
			}
			if ((options & IExtendedFileStore.DETAILED) != 0) {
				if (result != null && !result.isDir() && name != null && result.lastModified().getSeconds() == 0) {
					if (serverSupportsFeature("MDTM")) { //$NON-NLS-1$
						changeCurrentDir(dirPath);
						Policy.checkCanceled(monitor);
						try {
							Date lastModifiedLocalTZ = ftpClient.modtime(name);
							if (lastModifiedLocalTZ != null) {
								result.setLastModified(lastModifiedLocalTZ);
							}
						} catch (FTPException e) {
						}
					}
				}
			}
			if (result == null && Path.ROOT.equals(path)) {
				result = new FTPFile(StringUtils.EMPTY, path.toPortableString(), 0, true, new Date(0));
			}
			if (result != null) {
				return createFileInfo(result);
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_fetch_failed, e));			
		}
		ExtendedFileInfo fileInfo = new ExtendedFileInfo(path.lastSegment());
		fileInfo.setExists(false);
		return fileInfo;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#fetchFiles(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected ExtendedFileInfo[] fetchFiles(IPath path, int options, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		monitor = Policy.subMonitorFor(monitor, 1);
		try {
			FTPFile[] ftpFiles = listFiles(path, monitor);
			monitor.beginTask(Messages.FTPConnectionFileManager_gethering_file_details, ftpFiles.length);
			List<ExtendedFileInfo> list = new ArrayList<ExtendedFileInfo>();
			for (FTPFile ftpFile : ftpFiles) {
				if (".".equals(ftpFile.getName()) || "..".equals(ftpFile.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
					monitor.worked(1);
					continue;
				}
				Date lastModifiedServerInLocalTZ = ftpFile.lastModified();
				if (serverTimeZoneShift != 0) {
					ftpFile.setLastModified(new Date(lastModifiedServerInLocalTZ.getTime()+serverTimeZoneShift));
				}
				if ((options & IExtendedFileStore.DETAILED) != 0) {
					if (!ftpFile.isDir() && ftpFile.lastModified().getSeconds() == 0) {
						if (serverSupportsFeature("MDTM")) { //$NON-NLS-1$
							changeCurrentDir(path);
							Policy.checkCanceled(monitor);
							try {
								Date lastModifiedLocalTZ = ftpClient.modtime(ftpFile.getName());
								if (lastModifiedLocalTZ != null) {
									ftpFile.setLastModified(lastModifiedLocalTZ);
								}
							} catch (FTPException e) {
							}
						}
					}
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
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_fetching_directory_failed, e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#listDirectory(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
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
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_listing_directory_failed, e));			
		} finally {
			monitor.done();
		}
	}
		
	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#readFile(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected InputStream readFile(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		monitor.beginTask(Messages.FTPConnectionFileManager_initiating_download, 4);
		FTPClient downloadFtpClient = createFTPClient();
		try {
			initAndAuthFTPClient(downloadFtpClient, monitor);
			Policy.checkCanceled(monitor);
			setMessageLogger(downloadFtpClient, messageLogWriter);
			downloadFtpClient.setType(IFTPConstants.TRANSFER_TYPE_ASCII.equals(transferType)
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
						new FTPInputStream(downloadFtpClient, path.lastSegment()));
			} catch (FTPException e) {
				throwFileNotFound(e, path);
				return null;
			}
		} catch (Exception e) {
			setMessageLogger(downloadFtpClient, null);
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
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_opening_file_failed, e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#writeFile(org.eclipse.core.runtime.IPath, long, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected OutputStream writeFile(IPath path, long permissions, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		monitor.beginTask(Messages.FTPConnectionFileManager_initiating_file_upload, 4);
		FTPClient uploadFtpClient = createFTPClient();
		try {
			initAndAuthFTPClient(uploadFtpClient, monitor);
			Policy.checkCanceled(monitor);
			setMessageLogger(uploadFtpClient, messageLogWriter);
			uploadFtpClient.setType(IFTPConstants.TRANSFER_TYPE_ASCII.equals(transferType)
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
					new FTPOutputStream(uploadFtpClient, generateTempFileName(path.lastSegment())),
					path.lastSegment(), null, permissions);
		} catch (Exception e) {
			setMessageLogger(uploadFtpClient, null);
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
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_opening_file_failed, e));			
		} finally {
			monitor.done();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#deleteDirectory(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
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
				MultiStatus multiStatus = new MultiStatus(SecureFTPPlugin.PLUGIN_ID, 0, Messages.FTPConnectionFileManager_deleting_directory_failed, e);
				multiStatus.addAll(status);
			} else {
				throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_deleting_directory_failed, e));
			}
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#deleteFile(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
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
				System.out.println(e);
				throw e;
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID,
					StringUtils.format(Messages.FTPConnectionFileManager_deleting_failed, path), e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#createDirectory(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
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
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_creating_directory_failed, e));			
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#renameFile(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void renameFile(IPath sourcePath, IPath destinationPath, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		try {
			changeCurrentDir(Path.ROOT);
			Policy.checkCanceled(monitor);
			try {
				ftpClient.rename(sourcePath.toPortableString(), destinationPath.toPortableString());
			} catch (FTPException e) {
				throwFileNotFound(e, sourcePath);
				System.out.println(e);
				throw e;
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_renaming_failed, e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#setModificationTime(org.eclipse.core.runtime.IPath, long, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void setModificationTime(IPath path, long modificationTime, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		if (!serverSupportsFeature("MFMT")) { //$NON-NLS-1$
			return;
		}
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
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, Messages.FTPConnectionFileManager_set_modification_time_failed, e));			
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#changeFilePermissions(org.eclipse.core.runtime.IPath, long, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void changeFilePermissions(IPath path, long permissions, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		if (chmodSupported == Boolean.FALSE) {
			return;
		}
		try {
			IPath dirPath = path.removeLastSegments(1);
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			chmodSupported = ftpClient.site("CHMOD "+Long.toOctalString(permissions)+" "+path.lastSegment()); //$NON-NLS-1$ //$NON-NLS-2$
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
	 * @see com.aptana.ide.core.ftp.BaseFTPConnectionFileManager#changeFileGroup(org.eclipse.core.runtime.IPath, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void changeFileGroup(IPath path, String group, IProgressMonitor monitor) throws CoreException, FileNotFoundException {
		if (chgrpSupported == Boolean.FALSE) {
			return;
		}
		try {
			IPath dirPath = path.removeLastSegments(1);
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			chgrpSupported = ftpClient.site("CHGRP "+group+" "+path.lastSegment()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (FileNotFoundException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, SecureFTPPlugin.PLUGIN_ID, "Set group failed", e));			
		} finally {
			monitor.done();
		}
	}

	private FTPFile[] ftpSTAT(String dirname) throws IOException, FTPException, ParseException {
        if (fileFactory == null) {
            try {
                fileFactory = new FTPFileFactory(ftpClient.system());
            }
            catch (FTPException ex) {
                fileFactory = new FTPFileFactory(""); //$NON-NLS-1$
            }
            fileFactory.setLocales(FTPClient.DEFAULT_LISTING_LOCALES);
        }

        String[] validCodes = {"211", "212", "213"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		FTPReply reply = ftpClient.sendCommand("STAT "+dirname); //$NON-NLS-1$
		ftpClient.validateReply(reply, validCodes);
		String[] data = reply.getReplyData();
		for (int i = 0; i < data.length; ++i) {
			data[i] = data[i].trim();
		}
		return fileFactory.parse(data);
	}
	
	private FTPFile[] listFiles(IPath dirPath, IProgressMonitor monitor) throws IOException, ParseException, FTPException {
		if (statSuppoted != Boolean.FALSE) {
			FTPFile[] ftpFiles = null;
			try {
				ftpFiles = ftpSTAT(dirPath.addTrailingSeparator().toPortableString());
			} catch (FTPException e) {
				if (e.getReplyCode() == 502) {
					statSuppoted = null;
				} else if (e.getReplyCode() != 500) {
					throwFileNotFound(e, dirPath);
				}
			}
			if (statSuppoted == null && (ftpFiles == null || ftpFiles.length == 0)) {
				statSuppoted = Boolean.FALSE;
				Policy.checkCanceled(monitor);
				return listFiles(dirPath, monitor);
			} else if (statSuppoted == null) {
				statSuppoted = Boolean.TRUE;
			}
			if (ftpFiles.length == 1 && ftpFiles[0].getLinkedName() != null && dirPath.equals(Path.fromPortableString(ftpFiles[0].getName()))) {
				Policy.checkCanceled(monitor);
				changeCurrentDir(dirPath);
				Policy.checkCanceled(monitor);
				return ftpClient.dirDetails("-a"); //$NON-NLS-1$
			}
			return ftpFiles;
		} else {
			changeCurrentDir(dirPath);
			Policy.checkCanceled(monitor);
			return ftpClient.dirDetails("-a"); //$NON-NLS-1$
		}
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
							StringUtils.format(Messages.FTPConnectionFileManager_deleting_failed, path.append(name).toPortableString()), e));
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
					StringUtils.format(Messages.FTPConnectionFileManager_deleting_failed, path.toPortableString()), e));
		}
	}

	private static String generateTempFileName(String base) {
		StringBuffer sb = new StringBuffer();
		sb.append(TMP_UPLOAD_PREFIX).append(base);
		return sb.toString();
	}

}
