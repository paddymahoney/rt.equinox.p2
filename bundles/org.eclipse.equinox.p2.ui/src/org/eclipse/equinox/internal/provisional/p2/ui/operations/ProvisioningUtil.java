/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Sizing;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

/**
 * Utility methods for clients using the provisioning UI
 * 
 * @since 3.4
 */
public class ProvisioningUtil {

	public static void addMetadataRepository(URL location) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		manager.addRepository(location);
	}

	public static String getMetadataRepositoryProperty(URL location, String key) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		return manager.getRepositoryProperty(location, key);
	}

	public static boolean getMetadataRepositoryEnablement(URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			return false;
		return manager.isEnabled(location);
	}

	public static boolean getArtifactRepositoryEnablement(URL location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			return false;
		return manager.isEnabled(location);
	}

	public static IMetadataRepository loadMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		return manager.loadRepository(location, monitor);
	}

	public static IStatus validateMetadataRepositoryLocation(URL location, IProgressMonitor monitor) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		return manager.validateRepositoryLocation(location, monitor);
	}

	public static URL getRollbackRepositoryURL() throws ProvisionException {
		IDirector director = getDirector();
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		return director.getRollbackRepositoryLocation();
	}

	public static void removeMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		manager.removeRepository(location);
	}

	public static void addArtifactRepository(URL location) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		manager.addRepository(location);
	}

	public static String getArtifactRepositoryProperty(URL location, String key) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getRepositoryProperty(location, key);
	}

	public static IArtifactRepository loadArtifactRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IArtifactRepository repo = manager.loadRepository(location, monitor);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, location.toExternalForm()));
		}
		return repo;
	}

	public static void removeArtifactRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		manager.removeRepository(location);
	}

	public static IProfile addProfile(String profileId, Map properties, IProgressMonitor monitor) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.addProfile(profileId, properties);
	}

	public static void removeProfile(String profileId, IProgressMonitor monitor) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		profileRegistry.removeProfile(profileId);
	}

	public static IProfile[] getProfiles() throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.getProfiles();
	}

	public static IProfile getProfile(String id) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.getProfile(id);
	}

	public static URL[] getMetadataRepositories(int flags) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getKnownRepositories(flags);
	}

	public static void refreshMetadataRepositories(URL[] urls, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			try {
				manager.refreshRepository(urls[i], mon.newChild(100));
			} catch (ProvisionException e) {
				//ignore problematic repositories when refreshing
			}
		}
	}

	public static URL[] getArtifactRepositories(int flags) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getKnownRepositories(flags);
	}

	public static void refreshArtifactRepositories(URL[] urls, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			manager.refreshRepository(urls[i], mon.newChild(100));
		}
	}

	/*
	 * Get the plan for the specified install operation
	 */
	public static ProvisioningPlan getProvisioningPlan(ProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
		return getPlanner().getProvisioningPlan(request, context, monitor);
	}

	/*
	 * See what updates might be available for a single IU.
	 * Useful when checking for updates and letting the user decide
	 * which IU's to update.
	 */
	public static IInstallableUnit[] updatesFor(IInstallableUnit toUpdate, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(toUpdate);
		return getPlanner().updatesFor(toUpdate, new ProvisioningContext(), monitor);
	}

	/*
	 * See what updates might be available for the specified IU's.
	 * Useful for bulk update that can be directly passed to the engine.
	 */
	public static IInstallableUnit[] updatesFor(IInstallableUnit[] toUpdate, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(toUpdate);

		IPlanner planner = getPlanner();
		ArrayList allUpdates = new ArrayList();
		for (int i = 0; i < toUpdate.length; i++) {
			IInstallableUnit[] updates = planner.updatesFor(toUpdate[i], new ProvisioningContext(), monitor);
			for (int j = 0; j < updates.length; j++)
				allUpdates.add(updates[j]);
		}
		return (IInstallableUnit[]) allUpdates.toArray(new IInstallableUnit[allUpdates.size()]);
	}

	/*
	 * Get a plan for becoming
	 */
	public static ProvisioningPlan getRevertPlan(IInstallableUnit profileIU, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(profileIU);
		return getPlanner().getRevertPlan(profileIU, new ProvisioningContext(), monitor);
	}

	/*
	 * Get sizing info for the specified IU's
	 */
	public static Sizing getSizeInfo(ProvisioningPlan plan, String profileId, IProgressMonitor monitor) throws ProvisionException {
		SizingPhaseSet set = new SizingPhaseSet();
		IStatus status = getEngine().perform(getProfile(profileId), set, plan.getOperands(), null, monitor);
		if (status.isOK())
			return set.getSizing();
		return null;
	}

	public static IStatus performProvisioningPlan(ProvisioningPlan plan, PhaseSet phaseSet, IProfile profile, IProgressMonitor monitor) throws ProvisionException {
		PhaseSet set;
		if (phaseSet == null)
			set = new DefaultPhaseSet();
		else
			set = phaseSet;
		return getEngine().perform(profile, set, plan.getOperands(), null, monitor);
	}

	private static IEngine getEngine() throws ProvisionException {
		IEngine engine = (IEngine) ServiceHelper.getService(ProvUIActivator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoEngineFound);
		}
		return engine;
	}

	public static IPlanner getPlanner() throws ProvisionException {
		IPlanner planner = (IPlanner) ServiceHelper.getService(ProvUIActivator.getContext(), IPlanner.class.getName());
		if (planner == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoPlannerFound);
		}
		return planner;
	}

	public static IDirector getDirector() throws ProvisionException {
		IDirector director = (IDirector) ServiceHelper.getService(ProvUIActivator.getContext(), IDirector.class.getName());
		if (director == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoDirectorFound);
		}
		return director;
	}

	public static void setColocatedRepositoryEnablement(URL location, boolean enabled) {
		IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (metaManager != null)
			metaManager.setEnabled(location, enabled);
		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (artifactManager != null)
			artifactManager.setEnabled(location, enabled);
	}

	public static boolean isCategory(IInstallableUnit iu) {
		String isCategory = iu.getProperty(IInstallableUnit.PROP_TYPE_CATEGORY);
		return isCategory != null && Boolean.valueOf(isCategory).booleanValue();
	}
}