/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.Hashtable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.engine.CertificateChecker;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests for {@link CertificateChecker}.
 */
public class CertificateCheckerTest extends AbstractProvisioningTest {
	class CertificateTestService implements IServiceUI {
		public boolean unsignedReturnValue = true;
		public boolean wasPrompted = false;

		public AuthenticationInfo getUsernamePassword(String location) {
			return null;
		}

		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			return null;
		}

		public TrustInfo getTrustInfo(Certificate[][] untrustedChain, String[] unsignedDetail) {
			wasPrompted = true;
			return new TrustInfo(null, false, unsignedReturnValue);
		}

	}

	CertificateChecker checker;
	ServiceRegistration serviceReg;
	CertificateTestService serviceUI;
	File unsigned;

	protected void setUp() throws Exception {
		checker = new CertificateChecker();
		try {
			unsigned = TestData.getFile("CertificateChecker", "unsigned.jar");
		} catch (IOException e) {
			fail("0.99", e);
		}
		assertTrue("1.0", unsigned != null);
		assertTrue("1.0", unsigned.exists());
		// We need to ensure the test service has a higher ranking than
		// anything registered by the SDK via DS.
		serviceUI = new CertificateTestService();
		Hashtable properties = new Hashtable(1);
		properties.put(org.osgi.framework.Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		serviceReg = EngineActivator.getContext().registerService(IServiceUI.class.getName(), serviceUI, properties);
	}

	protected void tearDown() throws Exception {
		if (serviceReg != null)
			serviceReg.unregister();
	}

	/**
	 * Tests that installing unsigned content is not allowed when the policy says it must fail.
	 */
	public void testPolicyAllow() {
		try {
			//if the service is consulted it will say no
			serviceUI.unsignedReturnValue = false;
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_ALLOW);
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.OK, result.getSeverity());
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that installing unsigned content is not allowed when the policy says it must fail.
	 */
	public void testPolicyFail() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_FAIL);
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.ERROR, result.getSeverity());

		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that installing unsigned content with the "prompt" policy and the prompt succeeds.
	 */
	public void testPolicyPromptSuccess() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			serviceUI.unsignedReturnValue = true;
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.OK, result.getSeverity());
			assertTrue("1.1", serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that the default policy for unsigned content is to prompt.
	 */
	public void testPolicyDefault() {
		System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		serviceUI.unsignedReturnValue = true;
		checker.add(unsigned);
		IStatus result = checker.start();
		assertEquals("1.0", IStatus.OK, result.getSeverity());
		assertTrue("1.1", serviceUI.wasPrompted);
	}

	/**
	 * Tests that installing unsigned content with the "prompt" policy and the prompt says no.
	 */
	public void testPolicyPromptCancel() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			serviceUI.unsignedReturnValue = false;
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.CANCEL, result.getSeverity());
			assertTrue("1.1", serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that trust checks that occur in a headless environment are properly treated
	 * as permissive, but not persistent, the same way as it would be if the service registration
	 * were not there.
	 */
	public void testBug291049() {
		try {

			// Intentionally unregister our service so that we get whatever the default (or null) service is
			// in an SDK configuration.  
			if (serviceReg != null) {
				serviceReg.unregister();
				serviceReg = null;
			}
			checker.add(unsigned);
			// TODO need to add some untrusted files here, too.  To prove that we treated them as trusted temporarily

			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			IStatus result = checker.start();
			assertTrue("1.0", result.isOK());
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}
}
