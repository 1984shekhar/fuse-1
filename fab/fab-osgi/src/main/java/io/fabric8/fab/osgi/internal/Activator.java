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
package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import io.fabric8.fab.osgi.FabResolverFactory;
import io.fabric8.fab.osgi.FabURLHandler;
import io.fabric8.fab.osgi.ServiceConstants;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Activator for the fab protocol
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {
    private static Activator instance;

    private BundleContext bundleContext;

    private ConfigAdmin configAdmin;

    private List<ServiceRegistration> registrations = new LinkedList<ServiceRegistration>();

    private FabResolverFactoryImpl factory;

    public static OsgiModuleRegistry registry = new OsgiModuleRegistry();

    public static Activator getInstance() {
        return instance;
    }

    public static BundleContext getInstanceBundleContext() {
        Activator activator = getInstance();
        if (activator != null) {
            return activator.getBundleContext();
        }
        return null;
    }

    public Activator() {
        super();
        instance = this;
    }

    @Override
    public void start(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;

        configAdmin = new ConfigAdmin();
        configAdmin.open();

        File data = new File(System.getProperty("karaf.data", "."));
        registry.setDirectory(new File(data, "fab-module-registry"));
        registry.setConfigurationAdmin(configAdmin);
        registry.setPid("io.fabric8.fab.osgi.registry");
        registry.load();

        // Create and register the FabResolverFactory
        factory = new FabResolverFactoryImpl();
        factory.setBundleContext(bundleContext);
        factory.setConfigurationAdmin(configAdmin);
        registerFabResolverFactory(factory);

        // track FeaturesService for use by factory
        ServiceTracker featuresServiceTracker = new ServiceTracker(bundleContext, FeaturesService.class, this);
        featuresServiceTracker.open();

        // Create and register the fab: URL handler
        FabURLHandler handler = new FabURLHandler();
        handler.setFabResolverFactory(factory);
        handler.setServiceProvider(factory);
        registerURLHandler(handler);
    }

    /*
     * Register the URL handler
     */
    private void registerURLHandler(FabURLHandler handler) {
        if (bundleContext != null && handler != null) {
            Hashtable props = new Hashtable();
            props.put("url.handler.protocol", ServiceConstants.PROTOCOL_FAB);
            ServiceRegistration registration = bundleContext.registerService(URLStreamHandlerService.class, handler, props);
            registrations.add(registration);
        }
    }

    /*
     * Register the {@link FabResolverFactory}
     */
    private void registerFabResolverFactory(FabResolverFactoryImpl factory) {
        if (bundleContext != null && factory != null) {
            ServiceRegistration registration = bundleContext.registerService(FabResolverFactory.class, factory, null);
            registrations.add(registration);
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) {
                registration.unregister();
            }
        }
        registrations.clear();
        factory = null;
        configAdmin.close();
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public Object addingService(ServiceReference serviceReference) {
        Object service = bundleContext.getService(serviceReference);
        if (service instanceof FeaturesService) {
            factory.setFeaturesService((FeaturesService) service);
        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference serviceReference, Object o) {
        // properties have changed, no need to do anything here
    }

    @Override
    public void removedService(ServiceReference serviceReference, Object o) {
        if (o instanceof FeaturesService && factory != null) {
            FeaturesService service = (FeaturesService) o;
            if (service == factory.getFeaturesService()) {
                factory.setFeaturesService(null);
            }
        }

    }

    public class ConfigAdmin extends ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> implements ConfigurationAdmin {

        public ConfigAdmin() {
            super(bundleContext, ConfigurationAdmin.class, null);
        }

        private ConfigurationAdmin getConfigAdmin() throws IOException {
            try {
                ConfigurationAdmin ca = waitForService(5000l);
                if (ca != null) {
                    return ca;
                }
                throw new IllegalStateException("ConfigurationAdmin not present");
            } catch (InterruptedException e) {
                throw (IOException) new InterruptedIOException("ConfigurationAdmin not present").initCause(e);
            }
        }

        @Override
        public org.osgi.service.cm.Configuration createFactoryConfiguration(String factoryPid) throws IOException {
            return getConfigAdmin().createFactoryConfiguration(factoryPid);
        }

        @Override
        public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
            return getConfigAdmin().createFactoryConfiguration(factoryPid, location);
        }

        @Override
        public Configuration getConfiguration(String pid, String location) throws IOException {
            return getConfigAdmin().getConfiguration(pid, location);
        }

        @Override
        public Configuration getConfiguration(String pid) throws IOException {
            return getConfigAdmin().getConfiguration(pid);
        }

        @Override
        public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
            return getConfigAdmin().listConfigurations(filter);
        }
    }
}
