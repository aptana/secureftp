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
package com.aptana.ide.syncing.sftp;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.io.IVirtualFile;
import com.aptana.ide.core.ui.SWTUtils;

/**
 * @author Ingo Muschenetz
 * @author Michael Xia (modified to use JFace Dialog)
 */
public class SftpInfoDialog extends Dialog {

    private Text _where;
    private Text _kind;
    private Text _size;
    private Text _modified;

    private Group _groupGroup;
    private Text _group;
    private Text _owner;

    private Group _permissionsGroup;
    private Button _userRead;
    private Button _userWrite;
    private Button _userExecute;
    private Button _groupRead;
    private Button _groupWrite;
    private Button _groupExecute;
    private Button _allRead;
    private Button _allWrite;
    private Button _allExecute;

    private IVirtualFile _item;

    /**
     * Create the dialog.
     * 
     * @param parent
     *            the parent shell
     */
    public SftpInfoDialog(Shell parent) {
        super(parent);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    /**
     * Sets the item to show the information on.
     * 
     * @param item
     *            the file item
     */
    public void setItem(IVirtualFile item) {
        this._item = item;
    }

    /**
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.InfoDialog_Info);
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
     */
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);

        Shell shell = getShell();
        Shell parentShell = getShell().getParent().getShell();
        SWTUtils.centerAndPack(shell, parentShell);

        return control;
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createDialogArea(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        main.setLayout(layout);
        main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group group = new Group(main, SWT.NONE);
        group.setText(Messages.InfoDialog_General);
        layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2,
                1));

        Label label = new Label(group, SWT.NONE);
        label.setText(Messages.InfoDialog_Kind);
        _kind = new Text(group, SWT.READ_ONLY);

        label = new Label(group, SWT.NONE);
        label.setText(Messages.InfoDialog_Size);
        _size = new Text(group, SWT.READ_ONLY);

        label = new Label(group, SWT.NONE);
        label.setText(Messages.InfoDialog_Where);
        _where = new Text(group, SWT.READ_ONLY);

        label = new Label(group, SWT.NONE);
        label.setText(Messages.InfoDialog_Modified);
        _modified = new Text(group, SWT.READ_ONLY);

        _groupGroup = new Group(main, SWT.NONE);
        _groupGroup.setEnabled(false);
        _groupGroup.setText(Messages.SftpInfoDialog_LBL_OwnerGroup);
        layout = new GridLayout();
        layout.numColumns = 2;
        _groupGroup.setLayout(layout);
        _groupGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 2, 1));

        label = new Label(_groupGroup, SWT.NONE);
        label.setEnabled(false);
        label.setText(StringUtils
                .makeFormLabel(Messages.SftpInfoDialog_LBL_Owner));
        _owner = new Text(_groupGroup, SWT.BORDER);
        _owner.setEnabled(false);
        _owner.setTextLimit(200);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 50;
        _owner.setLayoutData(gridData);

        label = new Label(_groupGroup, SWT.NONE);
        label.setEnabled(false);
        label.setText(StringUtils
                .makeFormLabel(Messages.SftpInfoDialog_LBL_Group));
        _group = new Text(_groupGroup, SWT.BORDER);
        _group.setEnabled(false);
        _group.setTextLimit(200);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 50;
        this._group.setLayoutData(gridData);

        _permissionsGroup = new Group(main, SWT.NONE);
        _permissionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                true, false, 2, 1));
        _permissionsGroup.setEnabled(true);
        _permissionsGroup.setText(Messages.SftpInfoDialog_LBL_Permissions);
        layout = new GridLayout();
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 10;
        layout.numColumns = 4;
        _permissionsGroup.setLayout(layout);

        label = new Label(_permissionsGroup, SWT.NONE);
        label.setText(StringUtils
                .makeFormLabel(Messages.SftpInfoDialog_LBL_User));
        _userRead = new Button(_permissionsGroup, SWT.CHECK);
        _userRead.setText(Messages.SftpInfoDialog_LBL_Read);
        _userWrite = new Button(this._permissionsGroup, SWT.CHECK);
        _userWrite.setText(Messages.SftpInfoDialog_LBL_Write);
        _userExecute = new Button(_permissionsGroup, SWT.CHECK);
        _userExecute.setText(Messages.SftpInfoDialog_LBL_Execute);

        label = new Label(_permissionsGroup, SWT.NONE);
        label.setText(StringUtils
                .makeFormLabel(Messages.SftpInfoDialog_LBL_Group));
        _groupRead = new Button(_permissionsGroup, SWT.CHECK);
        _groupRead.setText(Messages.SftpInfoDialog_LBL_Read);
        _groupWrite = new Button(_permissionsGroup, SWT.CHECK);
        _groupWrite.setText(Messages.SftpInfoDialog_LBL_Write);
        _groupExecute = new Button(_permissionsGroup, SWT.CHECK);
        _groupExecute.setText(Messages.SftpInfoDialog_LBL_Execute);

        label = new Label(_permissionsGroup, SWT.NONE);
        label.setText(StringUtils
                .makeFormLabel(Messages.SftpInfoDialog_LBL_All));
        _allRead = new Button(_permissionsGroup, SWT.CHECK);
        _allRead.setText(Messages.SftpInfoDialog_LBL_Read);
        _allWrite = new Button(_permissionsGroup, SWT.CHECK);
        _allWrite.setText(Messages.SftpInfoDialog_LBL_Write);
        _allExecute = new Button(_permissionsGroup, SWT.CHECK);
        _allExecute.setText(Messages.SftpInfoDialog_LBL_Execute);

        setInitialFieldValues();

        return main;
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
        applyFieldValues();
        super.okPressed();
    }

    private void applyFieldValues() {
        long perms = 0;

        perms |= (_userRead.getSelection() ? 0400 : 0);
        perms |= (_userWrite.getSelection() ? 0200 : 0);
        perms |= (_userExecute.getSelection() ? 0100 : 0);
        perms |= (_groupRead.getSelection() ? 0040 : 0);
        perms |= (_groupWrite.getSelection() ? 0020 : 0);
        perms |= (_groupExecute.getSelection() ? 0010 : 0);
        perms |= (_allRead.getSelection() ? 0004 : 0);
        perms |= (_allWrite.getSelection() ? 0002 : 0);
        perms |= (_allExecute.getSelection() ? 0001 : 0);

        if (_item.getPermissions() != perms) {
            _item.setPermissions(perms);
        }
    }

    /**
     * Sets the initial values for the fields.
     */
    private void setInitialFieldValues() {
        _kind.setText(_item.isDirectory() ? Messages.InfoDialog_Folder
                : Messages.InfoDialog_File);
        _size.setText(_item.getSize() + Messages.InfoDialog_Bytes);
        _where.setText(_item.getAbsolutePath());

        String group = _item.getGroup();
        String owner = _item.getOwner();

        if (group != null && group.length() > 0 && owner != null
                && owner.length() > 0) {
            _group.setText(_item.getGroup());
            _owner.setText(_item.getOwner());

            long perms = _item.getPermissions();
            _userRead.setSelection((perms & 0400) != 0);
            _userWrite.setSelection((perms & 0200) != 0);
            _userExecute.setSelection((perms & 0100) != 0);
            _groupRead.setSelection((perms & 0040) != 0);
            _groupWrite.setSelection((perms & 0020) != 0);
            _groupExecute.setSelection((perms & 0010) != 0);
            _allRead.setSelection((perms & 0004) != 0);
            _allWrite.setSelection((perms & 0002) != 0);
            _allExecute.setSelection((perms & 0001) != 0);
        } else {
            _groupGroup.setEnabled(false);
            _permissionsGroup.setEnabled(false);
        }

        if (_item.getModificationMillis() != 0L) {
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG,
                    DateFormat.LONG);
            _modified.setText(df
                    .format(new Date(_item.getModificationMillis())));
        } else if (_item.getTimeStamp() != null) {
            _modified.setText(_item.getTimeStamp());
        }

    }

}
