package com.aptana.ide.syncing.ftps;

import com.aptana.ide.syncing.ftp.AdvancedFTPDialog;
import com.aptana.ide.syncing.ftp.FtpDialog;
import com.aptana.ide.syncing.ftp.FtpDialogFactory;

/**
 * FTPS Dialog Factory
 * 
 * @author Shalom Gibly
 */
public class FtpsDialogFactory extends FtpDialogFactory
{
	private static FtpsDialogFactory instance;

	/**
	 * Returns an instance of this factory.
	 * 
	 * @return A singleton instance of this factory.
	 */
	public static FtpsDialogFactory getInstance()
	{
		if (instance == null)
		{
			instance = new FtpsDialogFactory();
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
		return new AdvancedFtpsDialog(dialog);
	}
}
