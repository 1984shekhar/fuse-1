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
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateChildContainerOptions;
import org.fusesource.fabric.api.CreateContainerBasicOptions;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.FabricAuthenticationException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.jmx.MQManager;
import org.fusesource.fabric.api.jmx.MQTopologyDTO;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.utils.Strings;
import org.fusesource.fabric.utils.shell.ShellUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.io.IOException;
import java.util.List;

@Command(name = "mq-create", scope = "fabric", description = "Create a new broker")
public class MQCreate extends FabricCommand {

    @Argument(index = 0, required = true, description = "Broker name")
    protected String name = null;

    @Option(name = "--parent-profile", description = "The parent profile to extend")
    protected String parentProfile;

    @Option(name = "--property", aliases = {"-D"}, description = "Additional properties to define in the profile")
    List<String> properties;

    @Option(name = "--config", description = "Configuration to use")
    protected String config;

    @Option(name = "--data", description = "Data directory for the broker")
    protected String data;

    @Option(name = "--group", description = "Broker group")
    protected String group;

    @Option(name = "--networks", description = "Broker networks")
    protected String networks;

    @Option(name = "--networks-username", description = "Broker networks UserName")
    protected String networksUserName;

    @Option(name = "--networks-password", description = "Broker networks Password")
    protected String networksPassword;

    @Option(name = "--version", description = "The version id in the registry")
    protected String version = ZkDefs.DEFAULT_VERSION;

    @Option(name = "--create-container", multiValued = false, required = false, description = "Comma separated list of child containers to create with mq profile")
    protected String create;

    @Option(name = "--assign-container", multiValued = false, required = false, description = "Assign this mq profile to the following containers")
    protected String assign;

    @Option(name = "--jmx-user", multiValued = false, required = false, description = "The jmx user name of the parent container.")
    protected String username;

    @Option(name = "--jmx-password", multiValued = false, required = false, description = "The jmx password of the parent container.")
    protected String password;

    @Option(name = "--jvm-opts", multiValued = false, required = false, description = "Options to pass to the container's JVM.")
    protected String jvmOpts;

    @Override
    protected Object doExecute() throws Exception {
        MQTopologyDTO dto = createDTO();

        List<CreateContainerBasicOptions.Builder> builderList = MQManager.createProfilesAndContainerBuilders(dto, fabricService, "child");

        for (CreateContainerBasicOptions.Builder builder : builderList) {
            CreateContainerMetadata[] metadatas = null;
            try {
                metadatas = fabricService.createContainers(builder.build());
                ShellUtils.storeFabricCredentials(session, username, password);
            } catch (FabricAuthenticationException fae) {
                //If authentication fails, prompts for credentials and try again.
                if (builder instanceof CreateChildContainerOptions.Builder) {
                    CreateChildContainerOptions.Builder childBuilder = (CreateChildContainerOptions.Builder) builder;
                    promptForJmxCredentialsIfNeeded();
                    metadatas = fabricService.createContainers(childBuilder.jmxUser(username).jmxPassword(password).build());
                    ShellUtils.storeFabricCredentials(session, username, password);
                }
            }
        }
        return null;
    }

    private MQTopologyDTO createDTO() {
        if (Strings.isNullOrBlank(username)) {
            username = ShellUtils.retrieveFabricUser(session);
        }
        if (Strings.isNullOrBlank(password)) {
            password = ShellUtils.retrieveFabricUserPassword(session);
        }

        MQTopologyDTO dto = new MQTopologyDTO();
        dto.setAssign(assign);
        dto.setConfig(config);
        dto.setCreate(create);
        dto.setData(data);
        dto.setGroup(group);
        dto.setJvmOpts(jvmOpts);
        dto.setName(name);
        dto.setNetworks(networks);
        dto.setNetworksPassword(networksPassword);
        dto.setNetworksUserName(networksUserName);
        dto.setParentProfile(parentProfile);
        dto.setPassword(password);
        dto.setProperties(properties);
        dto.setUsername(username);
        dto.setVersion(version);
        return dto;
    }

    private void promptForJmxCredentialsIfNeeded() throws IOException {
        // If the username was not configured via cli, then prompt the user for the values
        if (username == null) {
            log.debug("Prompting user for jmx login");
            username = ShellUtils.readLine(session, "Jmx Login for " + fabricService.getCurrentContainerName() + ": ", false);
        }

        if (password == null) {
            password = ShellUtils.readLine(session, "Jmx Password for " + fabricService.getCurrentContainerName() + ": ", true);
        }
    }
}
