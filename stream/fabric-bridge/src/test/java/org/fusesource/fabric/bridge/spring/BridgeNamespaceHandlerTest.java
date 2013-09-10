/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.bridge.spring;


import junit.framework.Assert;

import org.fusesource.fabric.bridge.BridgeConnector;
import org.fusesource.fabric.bridge.GatewayConnector;
import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.model.BridgedDestination;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.fusesource.fabric.bridge.model.DispatchPolicy;
import org.fusesource.fabric.bridge.model.RemoteBridge;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BridgeNamespaceHandlerTest extends Assert {

	private static final int TEST_TIMEOUT = 5000;
	private static final Logger LOG = LoggerFactory.getLogger(BridgeNamespaceHandlerTest.class);
	private AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("test-spring-context.xml");

	@Test
	@Ignore("[FABRIC-555] Fix stream/fabric-bridge tests")
	public void testDispatchPolicyParser() {
		DispatchPolicy bean = applicationContext.getBean("mypolicy", DispatchPolicy.class);
		LOG.info(bean.toString());
		assertNotNull("Property messageConverter not set", bean.getMessageConverterRef());
	}

	@Test
	public void testBridgedDestination() {
		BridgedDestination bean = applicationContext.getBean("mydestinationwithref", BridgedDestination.class);
		LOG.info(bean.toString());
		assertNotNull("Property dispatchPolicyBeanName not set", bean.getDispatchPolicyRef());
		assertEquals("Property dispatchPolicy not set", TEST_TIMEOUT, bean.getDispatchPolicy().getBatchTimeout());

		bean = applicationContext.getBean("mydestinationwithnested", BridgedDestination.class);
		LOG.info(bean.toString());
		assertNull("Property dispatchPolicyBeanName is set", bean.getDispatchPolicyRef());
		assertEquals("Property dispatchPolicy not set", TEST_TIMEOUT, bean.getDispatchPolicy().getBatchTimeout());
	}

	@Test
	public void testBridgeDestinationsConfig() {
		BridgeDestinationsConfig bean = applicationContext.getBean("mydestinationsconfig", BridgeDestinationsConfig.class);
		LOG.info(bean.toString());
		assertEquals("Property dispatchPolicy not set", TEST_TIMEOUT, bean.getDispatchPolicy().getBatchTimeout());
		assertEquals("Property destinations not set", 2, bean.getDestinations().size());
	}

	@Test
	public void testBridgeBrokerConfig() {
		BrokerConfig bean = applicationContext.getBean("mybrokerconfig", BrokerConfig.class);
		LOG.info(bean.toString());
		assertNotNull("Property connectionFactoryRef not set", bean.getConnectionFactoryRef());
		assertNotNull("Property connectionFactory not set", bean.getConnectionFactory());
		assertNotNull("Property destinationResolverRef not set", bean.getDestinationResolverRef());
		assertNotNull("Property destinationResolver not set", bean.getDestinationResolver());
	}

	@Test
	public void testRemoteBridge() {
		RemoteBridge bean = applicationContext.getBean("myremotebridgewithref", RemoteBridge.class);
		LOG.info(bean.toString());
		assertNotNull("Property remoteBrokerRef not set", bean.getRemoteBrokerRef());
		assertNotNull("Property remoteBrokerConfig not set", bean.getRemoteBrokerConfig());
		assertNotNull("Property outboundDestinationsRef not set", bean.getOutboundDestinationsRef());
		assertNotNull("Property outboundDestinations not set", bean.getOutboundDestinations());

		bean = applicationContext.getBean("myremotebridgewithnested", RemoteBridge.class);
		LOG.info(bean.toString());
		assertNull("Property remoteBrokerRef is set", bean.getRemoteBrokerRef());
		assertNotNull("Property remoteBrokerConfig not set", bean.getRemoteBrokerConfig());
		assertNull("Property outboundDestinationsRef is set", bean.getOutboundDestinationsRef());
		assertNotNull("Property outboundDestinations not set", bean.getOutboundDestinations());

	}

	@Test
	public void testBridgeConnector() {
		BridgeConnector bean = applicationContext.getBean("mybridgeconnector", BridgeConnector.class);
		LOG.info(bean.toString());
		assertTrue("BridgeConnector not started", bean.isRunning());
        assertTrue("Inbound destinations not set", bean.getInboundDestinations().getDestinations().isEmpty());
        assertFalse("Outbound destinations not set", bean.getOutboundDestinations().getDestinations().isEmpty());
	}

	@Test
	public void testGatewayConnector() {
		GatewayConnector bean = applicationContext.getBean("mygatewayconnector", GatewayConnector.class);
		LOG.info(bean.toString());
		assertTrue("GatewayConnector not started", bean.isRunning());
        assertTrue("Inbound destinations not set", bean.getInboundDestinations().getDestinations().isEmpty());
        assertFalse("Outbound destinations not set", bean.getOutboundDestinations().getDestinations().isEmpty());
        assertTrue("Remote Bridges not set", bean.getRemoteBridges().size() == 2);
	}

    @Test
    public void testBridgeConnectorWithRefs() {
        BridgeConnector bean = applicationContext.getBean("mybridgeconnectorwithrefs", BridgeConnector.class);
        LOG.info(bean.toString());
        assertTrue("BridgeConnector not started", bean.isRunning());
        assertFalse("Inbound destinations not set", bean.getInboundDestinations().getDestinations().isEmpty());
        assertFalse("Outbound destinations not set", bean.getOutboundDestinations().getDestinations().isEmpty());
    }

    @Test
    public void testGatewayConnectorWithRefs() {
        GatewayConnector bean = applicationContext.getBean("mygatewayconnectorwithrefs", GatewayConnector.class);
        LOG.info(bean.toString());
        assertTrue("GatewayConnector not started", bean.isRunning());
        assertFalse("Inbound destinations not set", bean.getInboundDestinations().getDestinations().isEmpty());
        assertFalse("Outbound destinations not set", bean.getOutboundDestinations().getDestinations().isEmpty());
    }

}
