package com.aptana.ide.syncing.ftp;

import com.aptana.ide.syncing.ftp.AdvancedFTPDialog;
import com.aptana.ide.syncing.ftp.FtpDialog;
import com.aptana.ide.syncing.ftp.FtpDialogFactory;

/**
 * Pro FTP Dialog Factory
 * 
 * @author Shalom Gibly
 */
public class FtpProDialogFactory extends FtpDialogFactory
{
	private static FtpProDialogFactory instance;

	/**
	 * Returns an instance of this factory.
	 * 
	 * @return A singleton instance of this factory.
	 */
	public static FtpProDialogFactory getInstance()
	{
		if (instance == null)
		{
			instance = new FtpProDialogFactory();
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
		return new AdvancedFtpProDialog(dialog);
	}
}
