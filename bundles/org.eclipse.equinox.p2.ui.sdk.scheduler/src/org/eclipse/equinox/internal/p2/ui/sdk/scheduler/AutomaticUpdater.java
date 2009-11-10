/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventObject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.5
 */
public class AutomaticUpdater implements IUpdateListener {

	StatusLineCLabelContribution updateAffordance;
	IStatusLineManager statusLineManager;
	IInstallableUnit[] iusWithUpdates;
	String profileId;
	ProvisioningListener profileListener;
	AutomaticUpdatesPopup popup;
	boolean alreadyDownloaded = false;
	UpdateOperation operation;
	private static final String AUTO_UPDATE_STATUS_ITEM = "AutoUpdatesStatus"; //$NON-NLS-1$

	public AutomaticUpdater() {
		createProfileListener();
	}

	private void createProfileListener() {
		profileListener = new ProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof ProfileEvent) {
					ProfileEvent event = (ProfileEvent) o;
					if (event.getReason() == ProfileEvent.CHANGED && sameProfile(event.getProfileId())) {
						triggerNewUpdateNotification();
					}
				}
			}
		};
		getProvisioningUI().getSession().getProvisioningEventBus().addListener(profileListener);
	}

	private boolean sameProfile(String another) {
		if (another.equals(IProfileRegistry.SELF)) {
			another = getSession().getProfile(another).getProfileId();
		}
		if (profileId.equals(IProfileRegistry.SELF)) {
			profileId = getSession().getProfile(profileId).getProfileId();
		}
		return profileId.equals(another);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener
	 * #
	 * updatesAvailable(org.eclipse.equinox.internal.provisional.p2.updatechecker
	 * .UpdateEvent)
	 */
	public void updatesAvailable(final UpdateEvent event) {
		final boolean download = getPreferenceStore().getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY);
		profileId = event.getProfileId();
		iusWithUpdates = event.getIUs();
		validateIusToUpdate();
		alreadyDownloaded = false;

		// Create an update operation to reflect the new updates that are available.
		operation = new UpdateOperation(getSession(), iusWithUpdates);
		operation.setProfileId(event.getProfileId());
		operation.setRootMarkerKey(IProfile.PROP_PROFILE_ROOT_IU);
		IStatus status = operation.resolveModal(new NullProgressMonitor());

		if (!status.isOK() || operation.getPossibleUpdates().length == 0) {
			clearUpdatesAvailable();
			return;
		}
		// Download the items before notifying user if the
		// preference dictates.

		if (download) {
			Job job = new ProfileModificationJob(AutomaticUpdateMessages.AutomaticUpdater_AutomaticDownloadOperationName, getSession(), event.getProfileId(), operation.getProvisioningPlan(), new ProvisioningContext(), new DownloadPhaseSet(), false);
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent jobEvent) {
					IStatus jobStatus = jobEvent.getResult();
					if (jobStatus.isOK()) {
						alreadyDownloaded = true;
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								setUpdateAffordanceState(operation.getResolutionResult().isOK());
							}
						});
					} else if (jobStatus.getSeverity() != IStatus.CANCEL) {
						StatusManager.getManager().handle(jobStatus, StatusManager.LOG);
					}
				}
			});
			job.schedule();
		} else {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					setUpdateAffordanceState(operation.getResolutionResult().isOK());
				}
			});
		}

	}

	ProvisioningSession getSession() {
		return AutomaticUpdatePlugin.getDefault().getSession();
	}

	/*
	 * Use with caution, as this still start the whole UI bundle.  Shouldn't be used
	 * in any of the update checking code, only the code that presents updates when notified.
	 */
	ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	/*
	 * Filter out the ius that aren't visible to the user or are
	 * locked for updating.
	 */

	void validateIusToUpdate() {
		ArrayList list = new ArrayList();
		IProfile profile = getSession().getProfile(profileId);

		for (int i = 0; i < iusWithUpdates.length; i++) {
			try {
				if (validToUpdate(profile, iusWithUpdates[i]))
					list.add(iusWithUpdates[i]);
			} catch (OperationCanceledException e) {
				// Nothing to report
			}
		}
		iusWithUpdates = (IInstallableUnit[]) list.toArray(new IInstallableUnit[list.size()]);
	}

	// A proposed update is valid if it is still visible to the user as an
	// installed item (it is a root)
	// and if it is not locked for updating.
	private boolean validToUpdate(IProfile profile, IInstallableUnit iu) {
		int lock = IProfile.LOCK_NONE;
		boolean isRoot = false;
		try {
			String value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU);
			if (value != null)
				lock = Integer.parseInt(value);
			value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_ROOT_IU);
			isRoot = value == null ? false : Boolean.valueOf(value).booleanValue();
		} catch (NumberFormatException e) {
			// ignore and assume no lock
		}
		return isRoot && (lock & IProfile.LOCK_UPDATE) == 0;
	}

	Shell getWorkbenchWindowShell() {
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return activeWindow != null ? activeWindow.getShell() : null;

	}

	IStatusLineManager getStatusLineManager() {
		if (statusLineManager != null)
			return statusLineManager;
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow == null)
			return null;
		// YUCK! YUCK! YUCK!
		// IWorkbenchWindow does not define getStatusLineManager(), yet
		// WorkbenchWindow does
		try {
			Method method = activeWindow.getClass().getDeclaredMethod("getStatusLineManager", new Class[0]); //$NON-NLS-1$
			try {
				Object statusLine = method.invoke(activeWindow, new Object[0]);
				if (statusLine instanceof IStatusLineManager) {
					statusLineManager = (IStatusLineManager) statusLine;
					return statusLineManager;
				}
			} catch (InvocationTargetException e) {
				// oh well
			} catch (IllegalAccessException e) {
				// I tried
			}
		} catch (NoSuchMethodException e) {
			// can't blame us for trying.
		}

		IWorkbenchPartSite site = activeWindow.getActivePage().getActivePart().getSite();
		if (site instanceof IViewSite) {
			statusLineManager = ((IViewSite) site).getActionBars().getStatusLineManager();
		} else if (site instanceof IEditorSite) {
			statusLineManager = ((IEditorSite) site).getActionBars().getStatusLineManager();
		}
		return statusLineManager;
	}

	void updateStatusLine() {
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null)
			manager.update(true);
	}

	void createUpdateAffordance() {
		updateAffordance = new StatusLineCLabelContribution(AUTO_UPDATE_STATUS_ITEM, 5);
		updateAffordance.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				launchUpdate();
			}
		});
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null) {
			manager.add(updateAffordance);
			manager.update(true);
		}
	}

	void setUpdateAffordanceState(boolean isValid) {
		if (updateAffordance == null)
			return;
		if (isValid) {
			updateAffordance.setTooltip(AutomaticUpdateMessages.AutomaticUpdater_ClickToReviewUpdates);
			updateAffordance.setImage(AutomaticUpdatePlugin.getDefault().getImageRegistry().get((AutomaticUpdatePlugin.IMG_TOOL_UPDATE)));
		} else {
			updateAffordance.setTooltip(AutomaticUpdateMessages.AutomaticUpdater_ClickToReviewUpdatesWithProblems);
			updateAffordance.setImage(AutomaticUpdatePlugin.getDefault().getImageRegistry().get((AutomaticUpdatePlugin.IMG_TOOL_UPDATE_PROBLEMS)));
		}
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null) {
			manager.update(true);
		}
	}

	void checkUpdateAffordanceEnablement() {
		// We don't currently support enablement in the affordance,
		// so we hide it if it should not be enabled.
		if (updateAffordance == null)
			return;
		boolean shouldBeVisible = getProvisioningUI().hasScheduledOperations();
		if (updateAffordance.isVisible() != shouldBeVisible) {
			IStatusLineManager manager = getStatusLineManager();
			if (manager != null) {
				updateAffordance.setVisible(shouldBeVisible);
				manager.update(true);
			}
		}
	}

	void createUpdatePopup() {
		popup = new AutomaticUpdatesPopup(getWorkbenchWindowShell(), alreadyDownloaded, getPreferenceStore());
		popup.open();

	}

	void clearUpdatesAvailable() {
		if (updateAffordance != null) {
			IStatusLineManager manager = getStatusLineManager();
			if (manager != null) {
				manager.remove(updateAffordance);
				manager.update(true);
			}
			updateAffordance.dispose();
			updateAffordance = null;
		}
		if (popup != null) {
			popup.close(false);
			popup = null;
		}
	}

	public void launchUpdate() {
		getProvisioningUI().openUpdateWizard(getProvisioningUI().getDefaultParentShell(), true, operation, null);
	}

	/*
	 * The profile has changed. Make sure our toUpdate list is still valid and
	 * if there is nothing to update, get rid of the update popup and
	 * affordance.
	 */
	void triggerNewUpdateNotification() {
		Job notifyJob = new Job("Update validate job") { //$NON-NLS-1$
			public IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				// notify that updates are available for all roots.  We don't know for sure that
				// there are any, but this will cause everything to be rechecked
				updatesAvailable(new UpdateEvent(profileId, getProvisioningUI().getProfileRoots()));
				return Status.OK_STATUS;
			}
		};
		notifyJob.setSystem(true);
		notifyJob.setUser(false);
		notifyJob.setPriority(Job.LONG);
		notifyJob.schedule();
	}

	public void shutdown() {
		statusLineManager = null;
		if (profileListener != null) {
			getSession().getProvisioningEventBus().removeListener(profileListener);
			profileListener = null;
		}
	}

	IPreferenceStore getPreferenceStore() {
		return AutomaticUpdatePlugin.getDefault().getPreferenceStore();
	}

}
