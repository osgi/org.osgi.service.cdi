/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.test.cases.cdi.junit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Hashtable;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.test.cases.cdi.interfaces.Pojo;
import org.osgi.util.tracker.ServiceTracker;

public class DisableComponentTests extends AbstractTestCase {

	@Before
	@Override
	public void setUp() throws Exception {
		// as ConfigurationTests: exceptions of other threads, such as the
		// container's while it changes, must not fail an await
		Awaitility.doNotCatchUncaughtExceptionsByDefault();
		Awaitility.pollInSameThread();
		adminTracker = new ServiceTracker<>(bundleContext, ConfigurationAdmin.class, null);
		adminTracker.open();
		configurationAdmin = adminTracker.getService();

		Configuration[] configurations = configurationAdmin
				.listConfigurations("(service.pid=osgi.cdi.org.osgi.test.cases.cdi.tb8)");

		if (configurations != null) {
			for (Configuration configuration : configurations) {
				configuration.delete();
			}
		}
	}

	@After
	@Override
	public void tearDown() throws Exception {
		adminTracker.close();
	}

	@Test
	public void testDisableContainerComponent() throws Exception {
		Bundle tb8Bundle = installBundle("tb8.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s)(service.bundleid=%s))",
			Pojo.class.getName(),
			"ContainerBean",
				tb8Bundle.getBundleId());) {

			Pojo pojo = tracker.waitForService(startTimeout);

			assertNotNull(pojo);

			Configuration configurationA = null;

			try {
				configurationA = configurationAdmin.getConfiguration("osgi.cdi.org.osgi.test.cases.cdi.tb8", "?");

				Dictionary<String, Object> p1 = new Hashtable<>();
				// 152 Container Component: the container component is named
				// after the container id, by default 'osgi.cdi.' + the
				// Bundle-SymbolicName; disabling it disables all components
				p1.put("osgi.cdi." + tb8Bundle.getSymbolicName() + ".enabled",
						false);

				configurationA.update(p1);

				// the configuration reaches the container asynchronously
				await().untilAsserted(() -> assertNull(tracker.getService()));

				p1 = new Hashtable<>();
				p1.put("containerBean.enabled", true);

				configurationA.update(p1);

				await().untilAsserted(() -> assertNotNull(tracker.getService()));
			} finally {
				if (configurationA != null) {
					try {
						configurationA.delete();
					} catch (Exception e) {
						// ignore
					}
				}
				tb8Bundle.uninstall();
			}
		}
	}

	@Test
	public void testDisableSingleComponent() throws Exception {
		Bundle tb8Bundle = installBundle("tb8.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s)(service.bundleid=%s))",
			Pojo.class.getName(),
			"SingleComponentBean",
				tb8Bundle.getBundleId());) {

			Pojo pojo = tracker.waitForService(startTimeout);

			assertNotNull(pojo);

			Configuration configurationA = null;

			try {
				configurationA = configurationAdmin.getConfiguration("osgi.cdi.org.osgi.test.cases.cdi.tb8", "?");

				Dictionary<String, Object> p1 = new Hashtable<>();
				p1.put("singleComponentBean.enabled", false);

				configurationA.update(p1);

				// the configuration reaches the container asynchronously
				await().untilAsserted(() -> assertNull(tracker.getService()));

				p1 = new Hashtable<>();
				p1.put("singleComponentBean.enabled", true);

				configurationA.update(p1);

				await().untilAsserted(() -> assertNotNull(tracker.getService()));
			} finally {
				if (configurationA != null) {
					try {
						configurationA.delete();
					} catch (Exception e) {
						// ignore
					}
				}
				tb8Bundle.uninstall();
			}
		}
	}

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> adminTracker;
	private ConfigurationAdmin configurationAdmin;

}
