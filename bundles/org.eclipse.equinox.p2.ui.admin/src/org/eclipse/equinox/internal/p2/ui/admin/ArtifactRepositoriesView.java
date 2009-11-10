/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URI;
import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddArtifactRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.model.ArtifactRepositories;
import org.eclipse.equinox.internal.p2.ui.model.IRepositoryElement;
import org.eclipse.equinox.p2.operations.RemoveRepositoryJob;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * This view allows users to interact with artifact repositories
 * 
 * @since 3.4
 */
public class ArtifactRepositoriesView extends RepositoriesView {

	/**
	 * 
	 */
	public ArtifactRepositoriesView() {
		// constructor
	}

	protected Object getInput() {
		return new ArtifactRepositories(getProvisioningUI());
	}

	protected String getAddCommandLabel() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_AddRepositoryLabel;
	}

	protected String getAddCommandTooltip() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_AddRepositoryTooltip;
	}

	protected String getRemoveCommandTooltip() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryTooltip;
	}

	protected int openAddRepositoryDialog(Shell shell) {
		return new AddArtifactRepositoryDialog(shell, getProvisioningUI()).open();
	}

	protected RemoveRepositoryJob getRemoveOperation(Object[] elements) {
		ArrayList locations = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IRepositoryElement)
				locations.add(((IRepositoryElement) elements[i]).getLocation());
		}
		return new RemoveArtifactRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, getProvisioningUI().getSession(), (URI[]) locations.toArray(new URI[locations.size()]));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.RepositoriesView#getListenerEventTypes()
	 */
	protected int getListenerEventTypes() {
		return ProvUIProvisioningListener.PROV_EVENT_ARTIFACT_REPOSITORY;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.ProvView#refreshUnderlyingModel()
	 */
	protected void refreshUnderlyingModel() {
		getProvisioningUI().schedule(new RefreshArtifactRepositoriesOperation(ProvAdminUIMessages.ProvView_RefreshCommandLabel, getProvisioningUI().getSession(), 0), StatusManager.SHOW | StatusManager.LOG);
	}

}
