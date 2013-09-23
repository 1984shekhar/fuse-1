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

package org.fusesource.fabric.git.http;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.eclipse.jgit.http.server.GitServlet;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.git.GitListener;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@ThreadSafe
@Component(name = "org.fusesource.fabric.git.server", description = "Fabric Git HTTP Server Registration Handler", immediate = true) // Done
public final class GitHttpServerRegistrationHandler extends AbstractComponent implements GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHttpServerRegistrationHandler.class);

    private static final String REALM_PROPERTY_NAME = "realm";
    private static final String ROLE_PROPERTY_NAME = "role";
    private static final String DEFAULT_REALM = "karaf";
    private static final String DEFAULT_ROLE = "admin";

    private static final String KARAF_NAME = System.getProperty(SystemProperties.KARAF_NAME);

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final GitServlet gitServlet = new GitServlet();

    @GuardedBy("CopyOnWriteArrayList") private final List<GitListener> listeners = new CopyOnWriteArrayList<GitListener>();
    @GuardedBy("volatile") private volatile Group<GitNode> group;
    @GuardedBy("volatile") private volatile String gitRemoteUrl;

    @Activate
    void activate(ComponentContext context, Map<String, String> properties) {

        group = new ZooKeeperGroup(curator.get(), ZkPath.GIT.getPath(), GitNode.class);
        group.add(this);
        group.update(createState());
        group.start();

        String realm =  properties != null && properties.containsKey(REALM_PROPERTY_NAME) ? properties.get(REALM_PROPERTY_NAME) : DEFAULT_REALM;
        String role =  properties != null && properties.containsKey(ROLE_PROPERTY_NAME) ? properties.get(ROLE_PROPERTY_NAME) : DEFAULT_ROLE;
        try {
            HttpContext base = httpService.get().createDefaultHttpContext();
            HttpContext secure = new SecureHttpContext(base, realm, role);
            String basePath = System.getProperty("karaf.data") + File.separator + "git" + File.separator;
            String fabricGitPath = basePath + "fabric";
            File fabricRoot = new File(fabricGitPath);
            if (!fabricRoot.exists() && !fabricRoot.mkdirs()) {
                throw new FileNotFoundException("Could not found git root:" + basePath);
            }
            Dictionary<String, Object> initParams = new Hashtable<String, Object>();
            initParams.put("base-path", basePath);
            initParams.put("repository-root", basePath);
            initParams.put("export-all", "true");
            httpService.get().registerServlet("/git", gitServlet, initParams, secure);
        } catch (Exception e) {
            LOGGER.error("Error while registering git servlet", e);
        }
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        try {
            if (group != null) {
                group.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove git server from registry.", e);
        }
    }

    public void addGitListener(GitListener listener) {
        if (isValid()) {
            listeners.add(listener);
        }
    }

    public void removeGitListener(GitListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void groupEvent(Group<GitNode> group, GroupEvent event) {
        if (isValid()) {
            if (group.isMaster()) {
                LOGGER.info("Git repo is the master");
            } else {
                LOGGER.info("Git repo is not the master");
            }
            try {
                GitNode state = createState();
                group.update(state);

                String url = state.getUrl();
                try {
                    String actualUrl = getSubstitutedData(curator.get(), url);
                    if (actualUrl != null && !actualUrl.equals(gitRemoteUrl)) {
                        // lets notify listeners
                        gitRemoteUrl = actualUrl;
                        fireGitRemoteUrlChanged(actualUrl);
                    }

                    if (group.isMaster()) {
                        // lets register the current URL to ConfigAdmin
                        String pid = "org.fusesource.fabric.git";
                        try {
                            Configuration conf = configAdmin.get().getConfiguration(pid);
                            if (conf == null) {
                                LOGGER.warn("No configuration for pid " + pid);
                            } else {
                                Dictionary<String, Object> properties = conf.getProperties();
                                if (properties == null) {
                                    properties = new Hashtable<String, Object>();
                                }
                                properties.put("fabric.git.url", actualUrl);
                                conf.update(properties);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Setting pid " + pid + " config admin to: " + properties);
                                }
                            }
                        } catch (Throwable e) {
                            LOGGER.error("Could not load config admin for pid " + pid + ". Reason: " + e, e);
                        }
                    }
                } catch (URISyntaxException e) {
                    LOGGER.error("Could not resolve actual URL from " + url + ". Reason: " + e, e);
                }

            } catch (IllegalStateException e) {
                // Ignore
            }
        }
    }

    private void fireGitRemoteUrlChanged(String remoteUrl) {
        for (GitListener listener : listeners) {
            listener.onRemoteUrlChanged(remoteUrl);
        }
    }

    private GitNode createState() {
        String fabricRepoUrl = "${zk:" + KARAF_NAME + "/http}/git/fabric/";
        GitNode state = new GitNode();
        state.setId("fabric-repo");
        state.setUrl(fabricRepoUrl);
        state.setContainer(KARAF_NAME);
        if (group != null && group.isMaster()) {
            state.setServices(new String[] { fabricRepoUrl });
        }
        return state;
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.set(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.set(null);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.set(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.set(null);
    }

    void bindHttpService(HttpService service) {
        this.httpService.set(service);
    }

    void unbindHttpService(HttpService service) {
        this.httpService.set(null);
    }
}
