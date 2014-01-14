/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package io.fabric8.docker.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.fabric8.docker.api.support.DockerPropertyNamingStrategy;
import lombok.Data;

import java.util.Map;

@Data
@JsonNaming(DockerPropertyNamingStrategy.class)
public class ContainerInfo {
    @JsonProperty("ID")
    private String iD;
    private String created;
    private String path;
    private String[] args;
    private ContainerConfig config;
    private State state;
    private String image;
    private NetworkSettings networkSettings;
    private String sysInitPath;
    private String resolvConfPath;
    private String hostnamePath;
    private String hostsPath;
    private String name;
    private String driver;
    private Map<String, String> volumes;
    private Map<String, String> volumesRW;
    private HostConfig hostConfig;
}
