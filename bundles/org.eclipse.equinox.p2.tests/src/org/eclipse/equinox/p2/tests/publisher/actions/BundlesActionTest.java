/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - allow setting greedy through directive (bug 247099)
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.errorStatus;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.statusWithMessageWhich;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.easymock.EasyMock;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.*;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.IBundleShapeAdvice;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class BundlesActionTest extends ActionTest {
	private static final String OSGI = PublisherHelper.OSGI_BUNDLE_CLASSIFIER;
	private static final String OSGI_IDENTITY = "osgi.identity";
	private static final String JAVA_PACKAGE = "java.package";//$NON-NLS-1$
	private static final String JAVA_EE_1_4_REQ = "providedCapabilities.exists(pc | pc.namespace == 'osgi.ee' && pc.attributes ~= filter('(|(&(osgi.ee=JavaSE)(version=1.4))(&(osgi.ee=CDC/Foundation)(version=1.1)))'))";
	private static final String JAVA_EE_1_6_REQ = "providedCapabilities.exists(pc | pc.namespace == 'osgi.ee' && pc.attributes ~= filter('(&(osgi.ee=JavaSE)(version=1.6))'))";

	private static final String TEST1_IUD_NAME = "iud";//$NON-NLS-1$
	private static final String TEST1_PROVZ_NAME = "iuz";//$NON-NLS-1$
	private static final String TEST1_PROVBUNDLE_NAME = "test1";//$NON-NLS-1$
	private static final String TEST1_REQ_EE_FILTER = JAVA_EE_1_4_REQ;
	private static final String TEST2_REQ_A_NAME = "iua";//$NON-NLS-1$
	private static final String TEST2_REQ_B_NAME = "iub";//$NON-NLS-1$
	private static final String TEST2_REQ_C_NAME = "iuc";//$NON-NLS-1$
	private static final String TEST2_REQ_EE_FILTER = JAVA_EE_1_4_REQ;
	private static final String TEST2_PROV_Z_NAME = "iuz";//$NON-NLS-1$
	private static final String TEST2_PROV_Y_NAME = "iuy";//$NON-NLS-1$
	private static final String TEST2_PROV_X_NAME = "iux";//$NON-NLS-1$
	private static final String TEST2_PROV_BUNDLE_NAME = "test2";//$NON-NLS-1$
	private static final String TEST3_PROV_BUNDLE_NAME = "test3";//$NON-NLS-1$
	private static final String TEST4_PROV_BUNDLE_NAME = "test4";//$NON-NLS-1$
	private static final String TEST4_REQ_PACKAGE_OPTIONAL_NAME = "iue";//$NON-NLS-1$
	private static final String TEST4_REQ_PACKAGE_OPTGREEDY_NAME = "iuf";//$NON-NLS-1$
	private static final String TEST4_REQ_BUNDLE_OPTIONAL_NAME = "iug";//$NON-NLS-1$
	private static final String TEST4_REQ_BUNDLE_OPTGREEDY_NAME = "iuh";//$NON-NLS-1$
	private static final String TEST5_REQ_EE_FILTER = JAVA_EE_1_4_REQ;
	private static final String TEST5_PROV_BUNDLE_NAME = "test5";//$NON-NLS-1$
	private static final String TESTDYN_REQ_EE_FILTER = JAVA_EE_1_6_REQ;

	private static final File TEST_BASE = new File(TestActivator.getTestDataFolder(), "BundlesActionTest");//$NON-NLS-1$
	private static final File TEST_FILE1 = new File(TEST_BASE, TEST1_PROVBUNDLE_NAME);
	private static final File TEST_FILE2 = new File(TEST_BASE, TEST2_PROV_BUNDLE_NAME + ".jar");//$NON-NLS-1$
	private static final File TEST_FILE2_PACKED = new File(TEST_BASE, TEST2_PROV_BUNDLE_NAME + ".jar.pack.gz");//$NON-NLS-1$

	private static final String PROVBUNDLE_NAMESPACE = "org.eclipse.equinox.p2.iu";//$NON-NLS-1$
	private static final String TEST2_IU_A_NAMESPACE = OSGI;
	private static final String TEST2_IU_B_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_IU_C_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST1_IU_D_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_PROV_Z_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_PROV_Y_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST2_PROV_X_NAMESPACE = JAVA_PACKAGE;
	private static final String TEST1_PROV_Z_NAMESPACE = JAVA_PACKAGE;

	private final Version BUNDLE1_VERSION = Version.create("0.1.0");//$NON-NLS-1$
	private final Version BUNDLE2_VERSION = Version.create("1.0.0.qualifier");//$NON-NLS-1$
	private final Version BUNDLE3_VERSION = Version.create("0.1.0.qualifier");//$NON-NLS-1$
	private final Version BUNDLE4_VERSION = Version.create("2.0.1");//$NON-NLS-1$
	private final Version BUNDLE5_VERSION = Version.create("0.1.0.qualifier");//$NON-NLS-1$

	private final VersionRange DEFAULT_VERSION_RANGE = VersionRange.emptyRange;
	private final Version PROVBUNDLE2_VERSION = BUNDLE2_VERSION;
	private final Version TEST2_PROVZ_VERSION = Version.emptyVersion;
	private final Version TEST2_PROVY_VERSION = Version.emptyVersion;
	private final Version TEST2_PROVX_VERSION = Version.emptyVersion;
	private final VersionRange TEST2_IUA_VERSION_RANGE = VersionRange.emptyRange;
	private final VersionRange TEST2_IUB_VERSION_RANGE = VersionRange.emptyRange;
	private final VersionRange TEST2_IUC_VERSION_RANGE = new VersionRange(Version.create("1.0.0"), true, Version.MAX_VERSION, true);//$NON-NLS-1$
	private final VersionRange TEST1_IUD_VERSION_RANGE = new VersionRange(Version.create("1.3.0"), true, Version.MAX_VERSION, true);//$NON-NLS-1$

	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());

	private MultiCapture<ITouchpointAdvice> tpAdvice1, tpAdvice2;
	private MultiCapture<IUpdateDescriptorAdvice> udAdvice3;
	private MultiCapture<ICapabilityAdvice> capAdvice5;

	@Override
	public void setupPublisherInfo() {
		tpAdvice1 = new MultiCapture<>();
		tpAdvice2 = new MultiCapture<>();

		udAdvice3 = new MultiCapture<>();
		capAdvice5 = new MultiCapture<>();

		super.setupPublisherInfo();
	}

	public void testAll() throws Exception {
		File[] files = TEST_BASE.listFiles();
		testAction = new BundlesAction(files);
		setupPublisherResult();
		setupPublisherInfo();
		artifactRepository.setProperty(AbstractPublisherApplication.PUBLISH_PACK_FILES_AS_SIBLINGS, "true");//$NON-NLS-1$

		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor()));
		verifyBundlesAction();
		cleanup();
		debug("Completed BundlesActionTest.");//$NON-NLS-1$
	}

	public void testTranslationFragment() {
		File foo_fragment = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo.fragment");//$NON-NLS-1$
		File foo = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo");//$NON-NLS-1$
		BundlesAction bundlesAction = new BundlesAction(new File[] {foo_fragment});
		PublisherInfo info = new PublisherInfo();
		PublisherResult results = new PublisherResult();

		bundlesAction.perform(info, results, new NullProgressMonitor());
		Collection<IInstallableUnit> ius = results.getIUs(null, null);
		assertEquals("1.0", 1, ius.size());

		info = new PublisherInfo();
		results = new PublisherResult();
		bundlesAction = new BundlesAction(new File[] {foo});
		bundlesAction.perform(info, results, new NullProgressMonitor());
		ius = results.getIUs(null, null);
		assertEquals("2.0", 1, ius.size());
		QueryableArray queryableArray = new QueryableArray(ius.toArray(new IInstallableUnit[ius.size()]));
		IQueryResult<IInstallableUnit> result = queryableArray.query(QueryUtil.createIUQuery("foo"), null);
		assertEquals("3.1", 1, queryResultSize(result));
		IInstallableUnit iu = result.iterator().next();
		TranslationSupport utils = new TranslationSupport();
		utils.setTranslationSource(queryableArray);
		assertEquals("3.2", "English Foo", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME));

		bundlesAction = new BundlesAction(new File[] {foo_fragment});
		bundlesAction.perform(info, results, new NullProgressMonitor());
		ius = results.getIUs(null, null);
		assertEquals("2.0", 3, ius.size());
		queryableArray = new QueryableArray(ius.toArray(new IInstallableUnit[ius.size()]));
		result = queryableArray.query(QueryUtil.createIUQuery("foo"), null);
		assertEquals("2.1", 1, queryResultSize(result));
		iu = result.iterator().next();
		utils.setTranslationSource(queryableArray);
		assertEquals("2.2", "German Foo", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
	}

	private void verifyBundlesAction() throws Exception {
		// verify publisher result
		verifyBundle1();
		verifyBundle2();
		verifyBundle3();
		verifyBundle4();
		verifyBundle5();

		verifyArtifactRepository();
	}

	private void verifyArtifactRepository() throws Exception {
		IArtifactKey key2 = ArtifactKey.parse("osgi.bundle,test2,1.0.0.qualifier");//$NON-NLS-1$
		IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key2);

		// Should have one canonical and one packed
		assertTrue("1.0", descriptors.length == 2);

		int packedIdx;
		int canonicalIdx;
		if (IArtifactDescriptor.FORMAT_PACKED.equals(descriptors[0].getProperty(IArtifactDescriptor.FORMAT))) {
			packedIdx = 0;
			canonicalIdx = 1;
		} else {
			packedIdx = 1;
			canonicalIdx = 0;
		}

		try (ZipInputStream actual = artifactRepository.getZipInputStream(descriptors[canonicalIdx]); ZipInputStream expected = new ZipInputStream(new FileInputStream(TEST_FILE2))) {
			TestData.assertEquals(expected, actual);
		}

		InputStream packedActual = artifactRepository.getRawInputStream(descriptors[packedIdx]);
		InputStream packedExpected = new BufferedInputStream(new FileInputStream(TEST_FILE2_PACKED));
		TestData.assertEquals(packedExpected, packedActual);

		IArtifactKey key1 = ArtifactKey.parse("osgi.bundle,test1,0.1.0");//$NON-NLS-1$
		ZipInputStream zis = artifactRepository.getZipInputStream(key1);
		Map<String, Object[]> fileMap = getFileMap(new HashMap<>(), new File[] {TEST_FILE1}, new Path(TEST_FILE1.getAbsolutePath()));
		TestData.assertContains(fileMap, zis, true);
	}

	private void verifyBundle1() {
		ArrayList<IInstallableUnit> ius = new ArrayList<>(publisherResult.getIUs(TEST1_PROVBUNDLE_NAME, IPublisherResult.ROOT));
		assertTrue(ius.size() == 1);
		IInstallableUnit bundle1IU = ius.get(0);

		assertNotNull("1.0", bundle1IU);
		assertEquals("1.1", bundle1IU.getVersion(), BUNDLE1_VERSION);

		// check required capabilities
		Collection<IRequirement> requiredCapability = bundle1IU.getRequirements();
		verifyRequiredCapability(requiredCapability, TEST1_IU_D_NAMESPACE, TEST1_IUD_NAME, TEST1_IUD_VERSION_RANGE);
		verifyRequirement(requiredCapability, TEST1_REQ_EE_FILTER, 0, 1, true);
		assertEquals("2.0", 2, requiredCapability.size());

		// check provided capabilities
		Collection<IProvidedCapability> providedCapabilities = bundle1IU.getProvidedCapabilities();
		verifyProvidedCapability(providedCapabilities, PROVBUNDLE_NAMESPACE, TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI_IDENTITY, TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI, TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST1_PROV_Z_NAMESPACE, TEST1_PROVZ_NAME, TEST2_PROVZ_VERSION);
		verifyProvidedCapability(providedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "source", Version.create("1.0.0"));//$NON-NLS-1$//$NON-NLS-2$
		assertEquals("2.1", 5, providedCapabilities.size());

		Collection<ITouchpointData> data = bundle1IU.getTouchpointData();
		boolean found = false;
		for (ITouchpointData td : data) {
			ITouchpointInstruction configure = td.getInstruction("configure");
			if (configure == null)
				continue;
			String body = configure.getBody();
			if (body != null && body.indexOf("download.eclipse.org/releases/ganymede") > 0) {
				found = true;
			}
		}
		assertTrue("3.0", found);
	}

	private void verifyBundle2() {
		ArrayList<IInstallableUnit> ius = new ArrayList<>(publisherResult.getIUs(TEST2_PROV_BUNDLE_NAME, IPublisherResult.ROOT));
		assertTrue(ius.size() == 1);
		IInstallableUnit bundle2IU = ius.get(0);

		assertNotNull(bundle2IU);
		assertEquals(bundle2IU.getVersion(), BUNDLE2_VERSION);

		// check required capabilities
		Collection<IRequirement> requirements = bundle2IU.getRequirements();
		verifyRequiredCapability(requirements, TEST2_IU_A_NAMESPACE, TEST2_REQ_A_NAME, TEST2_IUA_VERSION_RANGE);
		verifyRequiredCapability(requirements, TEST2_IU_B_NAMESPACE, TEST2_REQ_B_NAME, TEST2_IUB_VERSION_RANGE);
		verifyRequiredCapability(requirements, TEST2_IU_C_NAMESPACE, TEST2_REQ_C_NAME, TEST2_IUC_VERSION_RANGE);
		verifyRequirement(requirements, TEST2_REQ_EE_FILTER, 0, 1, true);
		assertTrue(requirements.size() == 4 /*number of tested elements*/);

		// check provided capabilities
		Collection<IProvidedCapability> providedCapabilities = bundle2IU.getProvidedCapabilities();
		verifyProvidedCapability(providedCapabilities, PROVBUNDLE_NAMESPACE, TEST2_PROV_BUNDLE_NAME, PROVBUNDLE2_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI, TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION);
		verifyProvidedCapability(providedCapabilities, OSGI_IDENTITY, TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST2_PROV_Z_NAMESPACE, TEST2_PROV_Z_NAME, TEST2_PROVZ_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST2_PROV_Y_NAMESPACE, TEST2_PROV_Y_NAME, TEST2_PROVY_VERSION);
		verifyProvidedCapability(providedCapabilities, TEST2_PROV_X_NAMESPACE, TEST2_PROV_X_NAME, TEST2_PROVX_VERSION);
		verifyProvidedCapability(providedCapabilities, PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "bundle", Version.create("1.0.0"));//$NON-NLS-1$//$NON-NLS-2$
		assertEquals(7, providedCapabilities.size()); /*number of tested elements*/

		// check %bundle name is correct
		Map<String, String> prop = bundle2IU.getProperties();
		assertTrue(prop.get("org.eclipse.equinox.p2.name").toString().equalsIgnoreCase("%bundleName"));//$NON-NLS-1$//$NON-NLS-2$
		assertTrue(prop.get("org.eclipse.equinox.p2.provider").toString().equalsIgnoreCase("%providerName"));//$NON-NLS-1$//$NON-NLS-2$

		Collection<ITouchpointData> data = bundle2IU.getTouchpointData();
		boolean found = false;
		for (ITouchpointData td : data) {
			ITouchpointInstruction configure = td.getInstruction("configure");
			if (configure == null)
				continue;
			String body = configure.getBody();
			if (body != null && body.indexOf("download.eclipse.org/releases/ganymede") > 0) {
				found = true;
			}
		}
		assertFalse("3.0", found);

	}

	private void verifyBundle3() {
		// also a regression test for bug 393051: manifest headers use uncommon (but valid) capitalization
		ArrayList<IInstallableUnit> ius = new ArrayList<>(publisherResult.getIUs(TEST3_PROV_BUNDLE_NAME, IPublisherResult.ROOT));

		assertTrue(ius.size() == 1);
		IInstallableUnit bundle3IU = ius.get(0);

		IUpdateDescriptor updateDescriptor = bundle3IU.getUpdateDescriptor();
		String name = RequiredCapability.extractName(updateDescriptor.getIUsBeingUpdated().iterator().next());
		VersionRange range = RequiredCapability.extractRange(updateDescriptor.getIUsBeingUpdated().iterator().next());
		String description = updateDescriptor.getDescription();
		int severity = updateDescriptor.getSeverity();

		VersionRange expectedRange = new VersionRange("(0.0.1," + BUNDLE3_VERSION + "]");
		assertEquals(TEST3_PROV_BUNDLE_NAME, name);
		assertEquals(expectedRange, range);
		assertEquals("Some description about this update", description.trim());
		assertEquals(8, severity);
	}

	private void verifyBundle4() {
		ArrayList<IInstallableUnit> ius = new ArrayList<>(publisherResult.getIUs(TEST4_PROV_BUNDLE_NAME, IPublisherResult.ROOT));
		assertTrue(ius.size() == 1);
		IInstallableUnit bundle4IU = ius.get(0);

		assertNotNull("1.0", bundle4IU);
		assertEquals("1.1", bundle4IU.getVersion(), BUNDLE4_VERSION);

		// check required capabilities
		Collection<IRequirement> requirements = bundle4IU.getRequirements();
		verifyRequiredCapability(requirements, JAVA_PACKAGE, TEST4_REQ_PACKAGE_OPTIONAL_NAME, DEFAULT_VERSION_RANGE, 0, 1, false);
		verifyRequiredCapability(requirements, JAVA_PACKAGE, TEST4_REQ_PACKAGE_OPTGREEDY_NAME, DEFAULT_VERSION_RANGE, 0, 1, true);
		verifyRequiredCapability(requirements, OSGI, TEST4_REQ_BUNDLE_OPTIONAL_NAME, DEFAULT_VERSION_RANGE, 0, 1, false);
		verifyRequiredCapability(requirements, OSGI, TEST4_REQ_BUNDLE_OPTGREEDY_NAME, DEFAULT_VERSION_RANGE, 0, 1, true);
		assertEquals("2.0", 4, requirements.size());
	}

	private void verifyBundle5() {
		ArrayList<IInstallableUnit> ius = new ArrayList<>(publisherResult.getIUs(TEST5_PROV_BUNDLE_NAME, IPublisherResult.ROOT));
		assertTrue(ius.size() == 1);
		IInstallableUnit bundle5IU = ius.get(0);

		Collection<IRequirement> requirements = bundle5IU.getRequirements();
		verifyRequirement(requirements, TEST5_REQ_EE_FILTER, 0, 1, true);
		assertTrue(requirements.size() == 2);
		IRequirement requirement = requirements.iterator().next();

		int min = requirement.getMin();
		int max = requirement.getMax();

		assertTrue(min == 6);
		assertTrue(max == 7);
	}

	@Override
	public void cleanup() {
		super.cleanup();
		if (artifactRepository != null) {
			artifactRepository.removeAll(new NullProgressMonitor());
			artifactRepository = null;
		}
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		//super sets publisherInfo.getMetadataRepository and publisherInfo.getContextMetadataRepository
		super.insertPublisherInfoBehavior();
		Map<String, String> sarProperties = new HashMap<>();
		sarProperties.put("key1", "value1");//$NON-NLS-1$//$NON-NLS-2$
		sarProperties.put("key2", "value2");//$NON-NLS-1$//$NON-NLS-2$

		Map<String, String> sdkProperties = new HashMap<>();
		sdkProperties.put("key1", "value1");//$NON-NLS-1$//$NON-NLS-2$
		sdkProperties.put("key2", "value2");//$NON-NLS-1$//$NON-NLS-2$

		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH).anyTimes();
		expect(publisherInfo.getAdvice(null, false, null, null, ICapabilityAdvice.class)).andReturn(new ArrayList<>()).anyTimes();

		expectOtherAdviceQueries(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION);
		expectPropertyAdviceQuery(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION, sarProperties);
		expectUpdateDescriptorAdviceQuery(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION, null);
		expectTouchpointAdviceQuery(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION, tpAdvice1);

		expectOtherAdviceQueries(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION);
		expectPropertyAdviceQuery(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION, sdkProperties);
		expectUpdateDescriptorAdviceQuery(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION, null);
		expectTouchpointAdviceQuery(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION, tpAdvice2);

		expectOtherAdviceQueries(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION);
		expectPropertyAdviceQuery(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION, sarProperties);
		expectUpdateDescriptorAdviceQuery(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION, udAdvice3);
		expectTouchpointAdviceQuery(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION, null);

		expectOtherAdviceQueries(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION);
		expectPropertyAdviceQuery(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION, null);
		expectUpdateDescriptorAdviceQuery(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION, null);
		expectTouchpointAdviceQuery(TEST4_PROV_BUNDLE_NAME, BUNDLE4_VERSION, null);

		expectCapabilityAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, capAdvice5);
		expectOtherAdviceQueries(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION);
		expectPropertyAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, sarProperties);
		expectUpdateDescriptorAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, null);
		expectTouchpointAdviceQuery(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, null);

		//capture any touchpoint advice, and return the captured advice when the action asks for it
		publisherInfo.addAdvice(and(AdviceMatcher.adviceMatches(TEST1_PROVBUNDLE_NAME, BUNDLE1_VERSION, ITouchpointAdvice.class), capture(tpAdvice1)));
		EasyMock.expectLastCall().anyTimes();

		publisherInfo.addAdvice(and(AdviceMatcher.adviceMatches(TEST2_PROV_BUNDLE_NAME, BUNDLE2_VERSION, ITouchpointAdvice.class), capture(tpAdvice2)));
		EasyMock.expectLastCall().anyTimes();

		publisherInfo.addAdvice(and(AdviceMatcher.adviceMatches(TEST3_PROV_BUNDLE_NAME, BUNDLE3_VERSION, AdviceFileAdvice.class), capture(udAdvice3)));

		publisherInfo.addAdvice(and(AdviceMatcher.adviceMatches(TEST5_PROV_BUNDLE_NAME, BUNDLE5_VERSION, AdviceFileAdvice.class), capture(capAdvice5)));
		EasyMock.expectLastCall().anyTimes();
	}

	private void expectOtherAdviceQueries(String bundleName, Version bundleVersion) {
		expect(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, ICapabilityAdvice.class)).andReturn(Collections.EMPTY_LIST);
		expect(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, IAdditionalInstallableUnitAdvice.class)).andReturn(Collections.EMPTY_LIST);
		expect(publisherInfo.getAdvice(null, true, bundleName, bundleVersion, IBundleShapeAdvice.class)).andReturn(null);
	}

	private void expectCapabilityAdviceQuery(String bundleName, Version bundleVersion, Collection<ICapabilityAdvice> answer) {
		if (answer == null)
			answer = Collections.emptyList();
		expect(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, ICapabilityAdvice.class)).andReturn(answer);
	}

	private void expectUpdateDescriptorAdviceQuery(String bundleName, Version bundleVersion, Collection<IUpdateDescriptorAdvice> answer) {
		if (answer == null)
			answer = Collections.emptyList();
		expect(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, IUpdateDescriptorAdvice.class)).andReturn(answer);
	}

	private void expectTouchpointAdviceQuery(String bundleName, Version bundleVersion, Collection<ITouchpointAdvice> answer) {
		if (answer == null)
			answer = Collections.emptyList();
		expect(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, ITouchpointAdvice.class)).andReturn(answer).anyTimes();
	}

	private void expectPropertyAdviceQuery(String bundleName, Version bundleVersion, Map<String, String> answer) {
		List<IPropertyAdvice> propertyAdvices;
		if (answer != null)
			propertyAdvices = Collections.singletonList(createPropertyAdvice(answer));
		else
			propertyAdvices = Collections.emptyList();
		expect(publisherInfo.getAdvice(null, false, bundleName, bundleVersion, IPropertyAdvice.class)).andReturn(propertyAdvices).times(2);
	}

	private IPropertyAdvice createPropertyAdvice(Map<String, String> properties) {
		IPropertyAdvice mockAdvice = EasyMock.createMock(IPropertyAdvice.class);
		expect(mockAdvice.getInstallableUnitProperties((InstallableUnitDescription) EasyMock.anyObject())).andReturn(null).anyTimes();
		expect(mockAdvice.getArtifactProperties((IInstallableUnit) EasyMock.anyObject(), (IArtifactDescriptor) EasyMock.anyObject())).andReturn(properties).anyTimes();
		EasyMock.replay(mockAdvice);
		return mockAdvice;
	}

	public void testDynamicImport() throws Exception {
		File testData = getTestData("dymamicImport", "testData/dynamicImport");
		IInstallableUnit iu = BundlesAction.createBundleIU(BundlesAction.createBundleDescription(testData), null, new PublisherInfo());

		Collection<IRequirement> requirements = iu.getRequirements();
		verifyRequirement(requirements, TESTDYN_REQ_EE_FILTER, 0, 1, true);
		assertEquals(1, requirements.size());
	}

	public void testPublishBundlesWhereOneBundleIsInvalid() throws Exception {
		File[] bundleLocations = new File(TestActivator.getTestDataFolder(), "bug331683").listFiles();
		testAction = new BundlesAction(bundleLocations);
		setupPublisherResult();
		PublisherInfo info = new PublisherInfo();
		IStatus status = testAction.perform(info, publisherResult, new NullProgressMonitor());

		// overall status shall be error...
		assertThat(status, errorStatus());
		List<IStatus> childStatuses = Arrays.asList(status.getChildren());
		assertThat(childStatuses, hasItem(statusWithMessageWhich(containsString("The manifest line \"foo\" is invalid; it has no colon ':' character after the header key."))));
		assertThat(childStatuses.size(), is(1));

		// ... but the valid bundle must still be published
		Collection<IInstallableUnit> ius = publisherResult.getIUs("org.eclipse.p2.test.validManifest", IPublisherResult.ROOT);
		assertThat(ius.size(), is(1));
	}
}
