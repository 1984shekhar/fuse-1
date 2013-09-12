/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.service.jclouds;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.jclouds.karaf.core.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.create;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

/**
 * A {@link ConnectionStateListener} that makes sure that whenever it connect to a new ensemble, it updates it with the cloud
 * provider information that are present in the {
 * @link ConfigurationAdmin}.
 *
 * A typical use case is when creating a cloud ensemble and join it afterwards to update it after the join, with the
 * cloud provider information, so that the provider doesn't have to be registered twice.
 *
 * If for any reason the new ensemble already has registered information for a provider, the provider will be skipped.
 */
@Component(name = "org.fusesource.fabric.jclouds.bridge",
        description = "Fabric Jclouds Service Bridge",
        immediate = true)
@Service(ConnectionStateListener.class)
public class CloudProviderBridge implements ConnectionStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderBridge.class);

    private static final String COMPUTE_FILTER = "(service.factoryPid=org.jclouds.compute)";
    private static final String BLOBSTORE_FILTER = "(service.factoryPid=org.jclouds.blobstore)";

    @Reference
    private ConfigurationAdmin configurationAdmin;
    @Reference
    private CuratorFramework curator;


    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                this.curator = client;
                onConnected();
                break;
            default:
                onDisconnected();
        }
    }

    public void onConnected() {
       registerServices(COMPUTE_FILTER);
       registerServices(BLOBSTORE_FILTER);
    }

    public void onDisconnected() {

    }

    public void registerServices(String filter) {
        try {
            Configuration[] configurations = configurationAdmin.listConfigurations(filter);
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    Dictionary properties = configuration.getProperties();
                    if (properties != null) {
                        String name = properties.get(Constants.NAME) != null ? String.valueOf(properties.get(Constants.NAME)) : null;
                        String identity = properties.get(Constants.IDENTITY) != null ? String.valueOf(properties.get(Constants.IDENTITY)) : null;
                        String credential = properties.get(Constants.CREDENTIAL) != null ? String.valueOf(properties.get(Constants.CREDENTIAL)) : null;
                        if (name != null && identity != null && credential != null && getCurator().getZookeeperClient().isConnected()) {
                            if (exists(getCurator(), ZkPath.CLOUD_SERVICE.getPath(name)) == null) {
                                create(getCurator(), ZkPath.CLOUD_SERVICE.getPath(name));

                                Enumeration keys = properties.keys();
                                while (keys.hasMoreElements()) {
                                    String key = String.valueOf(keys.nextElement());
                                    String value = String.valueOf(properties.get(key));
                                    if (!key.equals("service.pid") && !key.equals("service.factoryPid")) {
                                        setData(getCurator(), ZkPath.CLOUD_SERVICE_PROPERTY.getPath(name, key), value);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve compute service information from configuration admin.", e);
        }
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}
