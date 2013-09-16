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
package org.fusesource.fabric.blueprint;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.zookeeper.CreateMode;
import org.fusesource.fabric.api.ModuleStatus;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextClosedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@Component(name = "org.fusesource.fabric.extender.listener.spring",
        description = "Fabric Spring Application Listener",
        immediate = true)
public class FabricSpringApplicationListener {

    private static final String EXTENDER_TYPE = "spring";

    @Reference
    private CuratorFramework curator;

    private ServiceRegistration registration;
    private BundleListener listener;

    @Activate
    public synchronized void init(BundleContext bundleContext) {
        listener = createListener(bundleContext);
        if (listener != null) {
            registration = bundleContext.registerService("org.springframework.osgi.context.event.OsgiBundleApplicationContextListener", listener, null);
            bundleContext.addBundleListener(listener);
        }
    }

    @Deactivate
    public synchronized void destroy(BundleContext bundleContext) {
        if (listener != null) {
             bundleContext.removeBundleListener(listener);
        }

        if (registration != null) {
            registration.unregister();
        }
        this.listener = null;
    }

    private BundleListener createListener(BundleContext bundleContext) {
        try {
            Class cl = getClass().getClassLoader().loadClass("org.fusesource.fabric.blueprint.FabricSpringApplicationListener$SpringApplicationListener");
            return (BundleListener) cl.getConstructor(CuratorFramework.class).newInstance(curator);
        } catch (Throwable t) {
            return null;
        }
    }


    public static class SpringApplicationListener extends BaseExtenderListener implements OsgiBundleApplicationContextListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(FabricBlueprintBundleListener.class);

        public SpringApplicationListener(CuratorFramework curator) {
            bindCurator(curator);
        }

        @Override
        public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
            long bundleId = event.getBundle().getBundleId();
            try {
                ModuleStatus moduleStatus = toModuleStatus(event);
                status.put(bundleId, moduleStatus);
                setData(getCurator(), ZkPath.CONTAINER_EXTENDER_BUNDLE.getPath(name, getExtenderType(), String.valueOf(bundleId)), moduleStatus.name(), CreateMode.EPHEMERAL);
                update();
            } catch (Exception e) {
                LOGGER.warn("Failed to write blueprint status of bundle {}.", bundleId, e);
            }
        }


        private ModuleStatus toModuleStatus(OsgiBundleApplicationContextEvent event) {
            if (event instanceof BootstrappingDependencyEvent) {
                OsgiServiceDependencyEvent de = ((BootstrappingDependencyEvent) event).getDependencyEvent();
                if (de instanceof OsgiServiceDependencyWaitStartingEvent) {
                    return ModuleStatus.WAITING;
                }
            } else if (event instanceof OsgiBundleContextFailedEvent) {
                return ModuleStatus.FAILED;
            } else if (event instanceof OsgiBundleContextRefreshedEvent) {
                return ModuleStatus.STARTED;
            } else if (event instanceof OsgiBundleContextClosedEvent) {
                return ModuleStatus.STOPPED;
            }
            return ModuleStatus.UNKNOWN;
        }

        @Override
        public String getExtenderType() {
            return EXTENDER_TYPE;
        }
    }
}
