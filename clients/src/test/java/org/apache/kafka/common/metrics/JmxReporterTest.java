/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.common.metrics;

import org.apache.kafka.common.metrics.stats.Avg;
import org.apache.kafka.common.metrics.stats.Total;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JmxReporterTest {

    @Test
    public void testJmxRegistration() throws Exception {
        Metrics metrics = new Metrics();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            metrics.addReporter(new JmxReporter());

            assertFalse(server.isRegistered(new ObjectName(":type=grp1")));

            Sensor sensor = metrics.sensor("kafka.requests");
            sensor.add(metrics.metricName("pack.bean1.avg", "grp1"), new Avg());
            sensor.add(metrics.metricName("pack.bean2.total", "grp2"), new Total());

            assertTrue(server.isRegistered(new ObjectName(":type=grp1")));
            assertEquals(0.0, server.getAttribute(new ObjectName(":type=grp1"), "pack.bean1.avg"));
            assertTrue(server.isRegistered(new ObjectName(":type=grp2")));
            assertEquals(0.0, server.getAttribute(new ObjectName(":type=grp2"), "pack.bean2.total"));

            metrics.removeMetric(metrics.metricName("pack.bean1.avg", "grp1"));

            assertFalse(server.isRegistered(new ObjectName(":type=grp1")));
            assertTrue(server.isRegistered(new ObjectName(":type=grp2")));
            assertEquals(0.0, server.getAttribute(new ObjectName(":type=grp2"), "pack.bean2.total"));

            metrics.removeMetric(metrics.metricName("pack.bean2.total", "grp2"));

            assertFalse(server.isRegistered(new ObjectName(":type=grp1")));
            assertFalse(server.isRegistered(new ObjectName(":type=grp2")));
        } finally {
            metrics.close();
        }
    }

    @Test
    public void testJmxRegistrationSanitization() throws Exception {
        Metrics metrics = new Metrics();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            metrics.addReporter(new JmxReporter());

            Sensor sensor = metrics.sensor("kafka.requests");
            sensor.add(metrics.metricName("name", "group", "desc", "id", "foo*"), new Total());
            sensor.add(metrics.metricName("name", "group", "desc", "id", "foo+"), new Total());
            sensor.add(metrics.metricName("name", "group", "desc", "id", "foo?"), new Total());
            sensor.add(metrics.metricName("name", "group", "desc", "id", "foo:"), new Total());

            assertTrue(server.isRegistered(new ObjectName(":type=group,id=foo%2A")));
            assertEquals(0.0, server.getAttribute(new ObjectName(":type=group,id=foo%2A"), "name"));
            assertTrue(server.isRegistered(new ObjectName(":type=group,id=foo%2B")));
            assertEquals(0.0, server.getAttribute(new ObjectName(":type=group,id=foo%2B"), "name"));
            assertTrue(server.isRegistered(new ObjectName(":type=group,id=foo%3F")));
            assertEquals(0.0, server.getAttribute(new ObjectName(":type=group,id=foo%3F"), "name"));
            assertTrue(server.isRegistered(new ObjectName(":type=group,id=foo%3A")));
            assertEquals(0.0, server.getAttribute(new ObjectName(":type=group,id=foo%3A"), "name"));

            metrics.removeMetric(metrics.metricName("name", "group", "desc", "id", "foo*"));
            metrics.removeMetric(metrics.metricName("name", "group", "desc", "id", "foo+"));
            metrics.removeMetric(metrics.metricName("name", "group", "desc", "id", "foo?"));
            metrics.removeMetric(metrics.metricName("name", "group", "desc", "id", "foo:"));

            assertFalse(server.isRegistered(new ObjectName(":type=group,id=foo%2A")));
            assertFalse(server.isRegistered(new ObjectName(":type=group,id=foo%2B")));
            assertFalse(server.isRegistered(new ObjectName(":type=group,id=foo%3F")));
            assertFalse(server.isRegistered(new ObjectName(":type=group,id=foo%3A")));
        } finally {
            metrics.close();
        }
    }
}
