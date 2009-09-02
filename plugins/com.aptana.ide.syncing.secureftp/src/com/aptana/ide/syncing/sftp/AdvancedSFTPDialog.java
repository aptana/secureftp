package com.aptana.ide.syncing.sftp;

import com.aptana.ide.syncing.ftp.AdvancedFTPDialog;
import com.aptana.ide.syncing.ftp.FtpDialog;

/**
 * Advanced SFTP Dialog.
 * 
 * @author Shalom Gibly
 */
public class AdvancedSFTPDialog extends AdvancedFTPDialog
{

	/**
	 * Constructs a new advanced SFTP dialog.
	 * 
	 * @param ftpDialog
	 */
	public AdvancedSFTPDialog(FtpDialog ftpDialog)
	{
		super(ftpDialog);
	}

	/**
	 * @see com.aptana.ide.syncing.ftp.FtpDialog#initializeAdvancedFields()
	 */
	protected void initializeAdvancedFields()
	{
		super.initializeAdvancedFields();
		// Disable the passive mode button for the SFTP
		passiveMode.setEnabled(false);
	}
}
