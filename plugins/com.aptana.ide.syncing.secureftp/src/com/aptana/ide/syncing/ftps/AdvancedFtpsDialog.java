package com.aptana.ide.syncing.ftps;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.aptana.ide.core.ui.SWTUtils;
import com.aptana.ide.io.ftps.FtpsVirtualFileManager;
import com.aptana.ide.syncing.ftp.AdvancedFTPDialog;
import com.aptana.ide.syncing.ftp.FtpDialog;

/**
 * Advanced FTPS Dialog.
 * 
 * @author Shalom Gibly
 */
public class AdvancedFtpsDialog extends AdvancedFTPDialog
{
	private Button useExplicitModeButton;
	private Combo securityTypeCombo;

	/**
	 * Constructs a new AdvancedFtpsDialog
	 * 
	 * @param dialog
	 */
	public AdvancedFtpsDialog(FtpDialog dialog)
	{
		super(dialog);
	}

	/**
	 * @see com.aptana.ide.syncing.ftp.FtpDialog#createExtendAdvancedContent(org.eclipse.swt.widgets.Composite)
	 */
	protected void createExtendAdvancedContent(Composite parent)
	{
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		composite.setLayoutData(gridData);
		useExplicitModeButton = new Button(composite, SWT.CHECK);
		useExplicitModeButton.setText(Messages.AdvancedFtpsDialog_UseExplicitMode);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		useExplicitModeButton.setLayoutData(gridData);
		useExplicitModeButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(final SelectionEvent e)
			{
				String currentValue = port.getText();
				String oldDefault;
				String newDefault;

				if (useExplicitModeButton.getSelection())
				{
					newDefault = Integer.toString(FtpsVirtualFileManager.DEFAULT_EXPLICIT_FTPS_PORT);
					oldDefault = Integer.toString(FtpsVirtualFileManager.DEFAULT_IMPLICIT_FTPS_PORT);
					securityTypeCombo.setEnabled(true);
				}
				else
				{
					newDefault = Integer.toString(FtpsVirtualFileManager.DEFAULT_IMPLICIT_FTPS_PORT);
					oldDefault = Integer.toString(FtpsVirtualFileManager.DEFAULT_EXPLICIT_FTPS_PORT);
					securityTypeCombo.setEnabled(false);
				}

				// update the default value
				port.setText(newDefault);

				// set the text
				if (oldDefault.equals(currentValue))
				{
					// user had a default value before so change this to the new default value
					port.setText(newDefault);
				}
				else
				{
					// setFieldWithDefaultValue changes the field value to the default value
					// so we reset it here to prevent the change
					port.setText(currentValue);
				}
			}
		});

		Label security = new Label(composite, SWT.NONE);
		security.setText(Messages.AdvancedFtpsDialog_LBL_SecurityType);
		securityTypeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		securityTypeCombo.add(FtpsVirtualFileManager.SSL);
		securityTypeCombo.add(FtpsVirtualFileManager.TLS);
		securityTypeCombo.add(FtpsVirtualFileManager.TLS_C);
		securityTypeCombo.setText(FtpsVirtualFileManager.SSL);
	}

	/**
	 * @see com.aptana.ide.syncing.ftp.FtpDialog#initializeAdvancedFields()
	 */
	protected void initializeAdvancedFields()
	{
		super.initializeAdvancedFields();
		if (!(item instanceof FtpsVirtualFileManager))
		{
			throw new IllegalArgumentException("Expected FtpsVirtualFileManager and got " + item.getClass().getName()); //$NON-NLS-1$
		}
		FtpsVirtualFileManager ftpsManager = (FtpsVirtualFileManager) item;
		if (item.getPort() > 0) {	
		} else {
		if (ftpsManager.getExplicitMode())
		{
			SWTUtils.setTextWidgetValue(this.port, Integer
					.toString(FtpsVirtualFileManager.DEFAULT_EXPLICIT_FTPS_PORT));
			this.securityTypeCombo.setEnabled(true);
		}
		else
		{
			SWTUtils.setTextWidgetValue(this.port, Integer
					.toString(FtpsVirtualFileManager.DEFAULT_IMPLICIT_FTPS_PORT));
			this.securityTypeCombo.setEnabled(false);
		}
		}
		if (!ftpDialog.isNewItem())
		{
			this.useExplicitModeButton.setSelection(ftpsManager.getExplicitMode());
			if (ftpsManager.getSecurityType() != null)
			{
				this.securityTypeCombo.setText(ftpsManager.getSecurityType());
			}
		}
	}

	/**
	 * @see com.aptana.ide.syncing.ftp.FtpDialog#saveValues()
	 */
	public void saveValues()
	{
		super.saveValues();
		if (item instanceof FtpsVirtualFileManager)
		{
			FtpsVirtualFileManager manager = (FtpsVirtualFileManager) item;
			manager.setExplicitMode(this.useExplicitModeButton.getSelection());
			manager.setSecurityType(this.securityTypeCombo.getText());
		}
		else
		{
			throw new IllegalArgumentException("Expected FtpsVirtualFileManager and got " + item.getClass().getName()); //$NON-NLS-1$
		}
	}
}
