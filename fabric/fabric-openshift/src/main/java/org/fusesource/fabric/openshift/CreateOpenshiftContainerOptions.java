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
package org.fusesource.fabric.openshift;

import org.codehaus.jackson.annotate.JsonProperty;
import org.fusesource.fabric.api.CreateContainerBasicOptions;
import org.fusesource.fabric.api.CreateRemoteContainerOptions;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class CreateOpenshiftContainerOptions extends CreateContainerBasicOptions<CreateOpenshiftContainerOptions> implements CreateRemoteContainerOptions {
    private static final long serialVersionUID = 4489740280396972109L;

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {
        @JsonProperty
        private String serverUrl;
        @JsonProperty
        private String login;
        @JsonProperty
        private String password;
        @JsonProperty
        private String domain;
        @JsonProperty
        private String application;
        @JsonProperty
        private String gearProfile = "small";
        @JsonProperty
        private Map<String, String> environmentalVariables = new HashMap<String, String>();

        public Builder serverUrl(final String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder login(final String login) {
            this.login = login;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder domain(final String domain) {
            this.domain = domain;
            return this;
        }

        public Builder application(final String application) {
            this.application = application;
            return this;
        }

        public Builder gearProfile(final String gearProfile) {
            this.gearProfile = gearProfile;
            return this;
        }


        public Builder environmentalVariables(final Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
            return this;
        }


        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        public Map<String, String> getEnvironmentalVariables() {
            return environmentalVariables;
        }

        public void setEnvironmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
        }

        public CreateOpenshiftContainerOptions build() {
            return new CreateOpenshiftContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                    getMaximumPort(), getProfiles(), getVersion(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isAgentEnabled(), isAutoImportEnabled(),
                    getImportPath(), getUsers(), getName(), getParent(), "openshift", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                    getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), serverUrl, login, password, domain, application, gearProfile, environmentalVariables);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty
    private final String serverUrl;
    @JsonProperty
    private final String login;
    @JsonProperty
    private final String password;
    @JsonProperty
    private final String domain;
    @JsonProperty
    private final String application;
    @JsonProperty
    private final String gearProfile;
    @JsonProperty
    private final Map<String, String> environmentalVariables;


    public CreateOpenshiftContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, int getZooKeeperServerPort, int zooKeeperServerConnectionPort, String zookeeperPassword, boolean agentEnabled, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, Integer number, URI proxyUri, String zookeeperUrl, String jvmOpts, boolean adminAccess, String serverUrl, String login, String password, String domain, String application, String gearProfile, Map<String, String> environmentalVariables) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, getZooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, agentEnabled, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, adminAccess);
        this.serverUrl = serverUrl;
        this.login = login;
        this.password = password;
        this.domain = domain;
        this.application = application;
        this.gearProfile = gearProfile;
        this.environmentalVariables = environmentalVariables;
    }

    @Override
    public CreateOpenshiftContainerOptions updateCredentials(String user, String credential) {
        return new CreateOpenshiftContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isAgentEnabled(), isAutoImportEnabled(),
                getImportPath(), getUsers(), getName(), getParent(), "openshift", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), serverUrl, user, password, domain, application, gearProfile, environmentalVariables);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getDomain() {
        return domain;
    }

    public String getApplication() {
        return application;
    }

    public String getGearProfile() {
        return gearProfile;
    }

    @Override
    public String getHostNameContext() {
        return "openshift";
    }

    @Override
    public String getPath() {
        return "~/";
    }

    @Override
    public Map<String, String> getEnvironmentalVariables() {
        return environmentalVariables;
    }
}
