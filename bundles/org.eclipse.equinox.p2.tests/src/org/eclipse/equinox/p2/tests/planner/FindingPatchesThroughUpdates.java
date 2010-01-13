/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

import java.util.Properties;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class FindingPatchesThroughUpdates extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a120;
	IInstallableUnitPatch patchA1, patchA2, anotherPatch2, anotherPatch3;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"));
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor("A", new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		a120 = createIU("UpdateA", Version.createOSGi(1, 2, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);

		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability lifeCycle = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.2.0]"), null, false, false);
		patchA1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle);

		IRequirementChange change2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability lifeCycle2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 3.2.0]"), null, false, false);
		patchA2 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change2}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle2);

		IRequirementChange change3 = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability lifeCycle3 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 3.2.0]"), null, false, false);
		anotherPatch2 = createIUPatch("ANOTHERPATCH", Version.create("1.0.0"), true, new IRequirementChange[] {change3}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle3);

		IRequirementChange change4 = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability lifeCycle4 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 3.2.0]"), null, false, false);
		anotherPatch3 = createIUPatch("ANOTHERPATCH", Version.create("2.0.0"), null, NO_REQUIRES, NO_PROVIDES, new Properties(), null, null, true, MetadataFactory.createUpdateDescriptor("ANOTHERPATCH", new VersionRange("[1.0.0, 1.0.0]"), 0, ""), new IRequirementChange[] {change4}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle4, NO_REQUIRES);

		createTestMetdataRepository(new IInstallableUnit[] {a1, a120, patchA1, patchA2, anotherPatch2, anotherPatch3});

		planner = createPlanner();
	}

	public void testInstall() {
		IInstallableUnit[] updates = planner.updatesFor(a1, new ProvisioningContext(), new NullProgressMonitor());
		assertEquals(2, updates.length);
		assertEquals("Checking updates", new IInstallableUnit[] {a120, patchA1}, updates, false);
	}

	public void testFindUpdatesOfPatches() {
		IInstallableUnit[] updates = planner.updatesFor(anotherPatch2, new ProvisioningContext(), new NullProgressMonitor());
		assertEquals(1, updates.length);
		assertEquals("Checking updates", new IInstallableUnit[] {anotherPatch3}, updates, false);
	}
}