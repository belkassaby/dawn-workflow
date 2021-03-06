/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.ui.editors;

import java.io.File;

import org.dawb.common.services.IUserInputService;
import org.dawb.common.ui.util.DialogUtils;
import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.passerelle.ui.views.ActorDebugView;
import org.dawb.workbench.jmx.IDeligateWorkbenchPart;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;

public class UserInputService extends AbstractServiceFactory implements IUserInputService {

	@Override
	public Object create(Class serviceInterface, IServiceLocator parentLocator,
			IServiceLocator locator) {
		if (IUserInputService.class == serviceInterface) {
			return new UserInputService();
		}
		return null;
	}

	@Override
	public IRemoteWorkbenchPart openUserInputPart(final String partName, boolean isDialog) throws Exception {
		
		IDeligateWorkbenchPart deligate = new UserModifyRemotePart();
		return openPart(partName, isDialog, deligate, new Point(550, 480), "org.dawb.passerelle.editors.UserModifyEditor");
	}

	@Override
	public IRemoteWorkbenchPart openUserPlotPart(String partName, boolean isDialog) throws Exception {
		
		IDeligateWorkbenchPart deligate = new UserPlotRemotePart();
		return openPart(partName, isDialog, deligate, new Point(1275, 800), "org.dawb.passerelle.editors.UserModifyEditor");
	}
	
	@Override
	public IRemoteWorkbenchPart openDebugPart(String partName, boolean isDialog) throws Exception {
		
		return (ActorDebugView)EclipseUtils.getActivePage().showView(ActorDebugView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
	}
	

	private IRemoteWorkbenchPart openPart(String partName, boolean isDialog, IDeligateWorkbenchPart deligate, final Point size, final String id) throws Exception {
		if (isDialog) {
			UserInputDialog dialog = new UserInputDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), deligate);
			dialog.create();
			dialog.getShell().setText(partName);
			dialog.getShell().setSize(size);
			if (Display.getDefault().getActiveShell()!=null && dialog.getShell()!=null) {
			    DialogUtils.centerDialog(Display.getDefault().getActiveShell(), dialog.getShell());
			}
		    return dialog;
		} else {
		    final IWorkbenchPage page = EclipseUtils.getActivePage();
		    final File tmp = File.createTempFile("review", "txt");
		    final IFileStore externalFile = EFS.getLocalFileSystem().fromLocalFile(tmp);
		    FileStoreEditorInput input = new FileStoreEditorInput(externalFile);
		    UserInputEditor ed = (UserInputEditor)page.openEditor(input, id);
		    ed.setDeligate(deligate);
		    return ed;
		}
	}


}
