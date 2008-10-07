package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.URLValidator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * RepositoryManipulatorDropTarget recognizes both URLTransfer and
 * FileTransfer data types.  Files are converted to URL's with the file
 * protocol.  Any dropped URLs (or Files) are interpreted to mean that the
 * user wishes to add these files as repositories.
 * 
 * @since 3.4
 *
 */
public class RepositoryManipulatorDropTarget extends URLDropAdapter {
	RepositoryManipulator manipulator;
	Control control;

	public RepositoryManipulatorDropTarget(RepositoryManipulator manipulator, Control control) {
		super(true); // convert file drops to URL
		Assert.isNotNull(manipulator);
		this.manipulator = manipulator;
		this.control = control;
	}

	protected void handleDrop(String urlText, final DropTargetEvent event) {
		event.detail = DND.DROP_NONE;
		final URL[] url = new URL[1];
		try {
			url[0] = new URL(urlText);
		} catch (MalformedURLException e) {
			ProvUI.reportStatus(URLValidator.getInvalidURLStatus(urlText), StatusManager.SHOW | StatusManager.LOG);
			return;
		}
		if (url[0] == null)
			return;

		Job job = new WorkbenchJob(ProvUIMessages.RepositoryManipulatorDropTarget_DragAndDropJobLabel) {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				IStatus status = manipulator.getRepositoryURLValidator(control.getShell()).validateRepositoryURL(url[0], false, monitor);
				if (status.isOK()) {
					ProvisioningOperation addOperation = manipulator.getAddOperation(url[0]);
					ProvisioningOperationRunner.schedule(addOperation, control.getShell(), StatusManager.SHOW | StatusManager.LOG);
					event.detail = DND.DROP_LINK;
				} else if (status.getCode() == URLValidator.ALTERNATE_ACTION_TAKEN) {
					event.detail = DND.DROP_COPY;
				} else if (status.getSeverity() == IStatus.CANCEL) {
					event.detail = DND.DROP_NONE;
				} else {
					status = new MultiStatus(ProvUIActivator.PLUGIN_ID, 0, new IStatus[] {status}, NLS.bind(ProvUIMessages.RepositoryManipulatorDropTarget_DragSourceNotValid, url[0].toExternalForm()), null);
					event.detail = DND.DROP_NONE;
				}
				return status;
			}
		};
		job.setPriority(Job.SHORT);
		job.setUser(true);
		job.schedule();
	}
}