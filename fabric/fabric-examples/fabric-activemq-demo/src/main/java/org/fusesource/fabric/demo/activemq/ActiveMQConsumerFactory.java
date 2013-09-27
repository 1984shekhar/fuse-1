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
package org.fusesource.fabric.demo.activemq;

import org.apache.felix.scr.annotations.*;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.mq.ActiveMQService;
import org.fusesource.mq.ConsumerThread;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.Map;

@Component(name = "org.fusesource.fabric.example.mq.consumer", description = "ActiveMQ Consumer Factory", immediate = true)
public class ActiveMQConsumerFactory extends AbstractComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQProducerFactory.class);
    ConsumerThread consumer;
    ActiveMQService consumerService;
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Activate
    void activate(ComponentContext context, Map<String, String> properties) throws Exception {
       updated(properties);
       activateComponent();
    }

    @Modified
    void updated(Map<String, String> properties) throws Exception {
        try {
            String brokerUrl = (String) properties.get("brokerUrl");
            if (brokerUrl == null) {
                brokerUrl = "discover:(fabric:default)";
            }
            String password = (String)properties.get("password");
            if (password == null) {
                password = fabricService.get().getZookeeperPassword();
            }
            consumerService = new ActiveMQService((String)properties.get("username"), password, brokerUrl);
            consumerService.setMaxAttempts(10);
            consumerService.start();
            String destination = (String) properties.get("destination");
            consumer = new ConsumerThread(consumerService, destination);
            consumer.start();
            LOG.info("Consumer started");
        } catch (JMSException e) {
            throw new Exception("Cannot start consumer", e);
        }
    }

    @Deactivate
    void deactivate() {
      destroy();
    }

    public void destroy() {
        if (consumer != null) {
            consumer.setRunning(false);
            consumerService.stop();
        }
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.set(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.set(null);
    }
}
