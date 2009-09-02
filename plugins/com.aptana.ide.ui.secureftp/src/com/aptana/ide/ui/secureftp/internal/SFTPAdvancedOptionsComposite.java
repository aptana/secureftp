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

package com.aptana.ide.ui.secureftp.internal;

import java.nio.charset.Charset;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.ui.PixelConverter;
import com.aptana.ide.filesystem.secureftp.ISFTPConstants;
import com.aptana.ide.filesystem.secureftp.ISFTPConnectionPoint;
import com.aptana.ide.ui.ftp.internal.IConnectionDialog;
import com.aptana.ide.ui.ftp.internal.IOptionsComposite;
import com.aptana.ide.ui.ftp.internal.NumberVerifyListener;
import com.aptana.ide.ui.io.dialogs.IDialogConstants;

/**
 * @author Max Stepanov
 *
 */
public class SFTPAdvancedOptionsComposite extends Composite implements IOptionsComposite {
	
	private IConnectionDialog connectionDialog;
	private Combo compressionCombo;
	private Text portText;
	private Combo encodingCombo;
	
	private ModifyListener modifyListener;
	
	/**
	 * @param parent
	 * @param style
	 */
	public SFTPAdvancedOptionsComposite(Composite parent, int style, IConnectionDialog connectionDialog) {
		super(parent, style);
		this.connectionDialog = connectionDialog;
		
		setLayout(GridLayoutFactory.swtDefaults().numColumns(5)
				.spacing(new PixelConverter(this).convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING),
						new PixelConverter(this).convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING))
				.create());

		/* row 1 */
		Label label = new Label(this, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(this).convertHorizontalDLUsToPixels(IDialogConstants.LABEL_WIDTH),
				SWT.DEFAULT).create());
		label.setText(StringUtils.makeFormLabel("Compression"));

		compressionCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		compressionCombo.add(ISFTPConstants.COMPRESSION_AUTO);
		compressionCombo.add(ISFTPConstants.COMPRESSION_NONE);
		compressionCombo.add(ISFTPConstants.COMPRESSION_ZLIB);
		compressionCombo.setLayoutData(GridDataFactory.swtDefaults().hint(
				compressionCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x, SWT.DEFAULT).create());

		label = new Label(this, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).hint(
				new PixelConverter(this).convertHorizontalDLUsToPixels(IDialogConstants.LABEL_WIDTH),
				SWT.DEFAULT).create());

		label = new Label(this, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().create());
		label.setText(StringUtils.makeFormLabel("Port"));
		
		portText = new Text(this, SWT.SINGLE | SWT.RIGHT | SWT.BORDER);
		portText.setLayoutData(GridDataFactory.swtDefaults().hint(
				Math.max(
						new PixelConverter(portText).convertWidthInCharsToPixels(5),
						portText.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x
					), SWT.DEFAULT).create());

		/* row 2 */
		label = new Label(this, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(this).convertHorizontalDLUsToPixels(IDialogConstants.LABEL_WIDTH),
				SWT.DEFAULT).create());
		label.setText(StringUtils.makeFormLabel("Encoding"));

		encodingCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		encodingCombo.setItems(Charset.availableCharsets().keySet().toArray(new String[0]));
		encodingCombo.setLayoutData(GridDataFactory.swtDefaults().hint(
				encodingCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x, SWT.DEFAULT)
				.span(4, 1).create());

		/* -- */
		addListeners();
		portText.addVerifyListener(new NumberVerifyListener());		
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.io.IPropertiesEditor#loadPropertiesFrom(java.lang.Object)
	 */
	public void loadPropertiesFrom(Object element) {
		Assert.isLegal(element instanceof ISFTPConnectionPoint);
		ISFTPConnectionPoint sftpConnectionPoint = (ISFTPConnectionPoint) element;

		removeListeners();
		try {
			int index = compressionCombo.indexOf(String.valueOf(sftpConnectionPoint.getCompression()));
			if (index >= 0) {
				compressionCombo.select(index);
			}
			portText.setText(Integer.toString(sftpConnectionPoint.getPort()));
			index = encodingCombo.indexOf(String.valueOf(sftpConnectionPoint.getEncoding()));
			if (index >= 0) {
				encodingCombo.select(index);
			}
		} finally {
			addListeners();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.io.IPropertiesEditor#savePropertiesTo(java.lang.Object)
	 */
	public boolean savePropertiesTo(Object element) {
		Assert.isLegal(element instanceof ISFTPConnectionPoint);
		boolean updated = false;
		ISFTPConnectionPoint sftpConnectionPoint = (ISFTPConnectionPoint) element;
		
		String compression = compressionCombo.getItem(compressionCombo.getSelectionIndex());
		if (!sftpConnectionPoint.getCompression().equals(compression)) {
			sftpConnectionPoint.setCompression(compression);
			updated = true;
		}
		int port = Integer.parseInt(portText.getText());
		if (sftpConnectionPoint.getPort() != port) {
			sftpConnectionPoint.setPort(port);
			updated = true;
		}
		String encoding = encodingCombo.getItem(encodingCombo.getSelectionIndex());
		if (!sftpConnectionPoint.getEncoding().equals(encoding)) {
			sftpConnectionPoint.setEncoding(encoding);
			updated = true;
		}
		return updated;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.ftp.internal.IOptionsComposite#isValid()
	 */
	public String isValid() {
		int port = 0;
		try {
			port = Integer.parseInt(portText.getText());
		} catch (NumberFormatException e) {
		}
		if (port <= 0 || port > Short.MAX_VALUE) {
			return "Please specify correct port number";
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.ftp.internal.IOptionsComposite#setValid(boolean)
	 */
	public void setValid(boolean valid) {
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.ftp.internal.IOptionsComposite#lockUI(boolean)
	 */
	public void lockUI(boolean lock) {
		compressionCombo.setEnabled(!lock);
		portText.setEnabled(!lock);
		encodingCombo.setEnabled(!lock);
	}
	
	protected void addListeners() {
		if (modifyListener == null) {
			modifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					connectionDialog.validate();
				}
			};
		}
		portText.addModifyListener(modifyListener);
	}
	
	protected void removeListeners() {
		if (modifyListener != null) {
			portText.removeModifyListener(modifyListener);
		}
	}

}
