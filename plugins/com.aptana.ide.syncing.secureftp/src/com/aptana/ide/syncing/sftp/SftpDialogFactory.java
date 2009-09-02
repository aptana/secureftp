package com.aptana.ide.syncing.sftp;

import com.aptana.ide.syncing.ftp.AdvancedFTPDialog;
import com.aptana.ide.syncing.ftp.FtpDialog;
import com.aptana.ide.syncing.ftp.FtpDialogFactory;

/**
 * SFTP Dialog Factory
 * 
 * @author Shalom Gibly
 */
public class SftpDialogFactory extends FtpDialogFactory
{
	private static SftpDialogFactory instance;

	/**
	 * Returns an instance of this factory.
	 * 
	 * @return A singleton instance of this factory.
	 */
	public static FtpDialogFactory getInstance()
	{
		if (instance == null)
		{
			instance = new SftpDialogFactory();
		}
		return instance;
	}

	/**
	 * Creates a new {@link AdvancedFTPDialog} for the given {@link FtpDialog}.
	 * 
	 * @param dialog
	 * @return a new AdvancedFTPDialog
	 */
	public AdvancedFTPDialog createAdvancedFtpDialog(FtpDialog dialog)
	{
		return new AdvancedSFTPDialog(dialog);
	}
}
