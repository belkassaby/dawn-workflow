/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 

package org.dawb.passerelle.common.parameter.function;

import org.dawb.common.ui.plot.function.FunctionEditTable;
import org.dawb.common.ui.plot.function.FunctionType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;

/**
 * A dialog for editing a Mathematical Function. Uses FunctionViewer table.
 *
 */
public class FunctionDialog extends Dialog {

	private static final Logger logger = LoggerFactory.getLogger(FunctionDialog.class);
	
	private FunctionEditTable functionEditor;
	private CCombo functionType;
	
	FunctionDialog(Shell parentShell, NamedObj container) {	
		super(parentShell);
	}
	
	protected boolean isResizable() {
		return true;
	}
	
	public Control createDialogArea(Composite parent) {
		
		final Composite main = (Composite)super.createDialogArea(parent);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final Composite top= new Composite(main, SWT.NONE);
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		top.setLayout(new GridLayout(2, false));
		
		final Label label = new Label(top, SWT.NONE);
		label.setText("Function type    ");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));	
		
		functionType = new CCombo(top, SWT.READ_ONLY|SWT.BORDER);
		functionType.setItems(FunctionType.getTypes());
		functionType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		functionType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					functionEditor.setFunction(FunctionType.createNew(functionType.getSelectionIndex()), null);
				} catch (Exception e1) {
					logger.error("Cannot create function "+FunctionType.getType(functionType.getSelectionIndex()).getName(), e1);
				}
			}
		});

		
		this.functionEditor = new FunctionEditTable();
		functionEditor.createPartControl(main);
		
		return main;
	}
	
	void setFunction(AFunction function) {
		final int index = FunctionType.getIndex(function.getClass());
		functionType.select(index);
		functionEditor.setFunction(function, null);
	}
	
	AFunction getFunction() {
		return functionEditor.getFunction();
	}
}
