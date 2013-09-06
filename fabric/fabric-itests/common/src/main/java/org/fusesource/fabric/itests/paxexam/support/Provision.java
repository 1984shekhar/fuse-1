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
package org.fusesource.fabric.itests.paxexam.support;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class Provision {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /**
     * Waits for all container to provision and reach the specified status.
     *
     * @param containers
     * @param status
     * @param timeout
     * @throws Exception
     */
    public static void containersStatus(Collection<Container> containers, String status, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForProvisionTasks = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (Container c : containers) {
            waitForProvisionTasks.add(completionService.submit(new WaitForProvisionTask(c, status, timeout)));
            sb.append(c.getId()).append(" ");
        }
        System.out.println("Waiting for containers: [" + sb.toString() + "] to successfully provision");
        for (int i = 0; i < containers.size(); i++) {
            completionService.poll(timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Wait for all container provision successfully provision and reach status success.
     * @param containers
     * @param timeout
     * @throws Exception
     */
    public static void containerStatus(Collection<Container> containers, Long timeout) throws Exception {
        containersStatus(containers, "success", timeout);
    }


    /**
     * Wait for all containers to become alive.
     * @param containers
     * @param alive
     * @param timeout
     * @throws Exception
     */
    public static void containersAlive(Collection<Container> containers, boolean alive, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForProvisionTasks = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (Container container : containers) {
            waitForProvisionTasks.add(completionService.submit(new WaitForAliveTask(container, alive, timeout)));
            sb.append(container.getId()).append(" ");
        }
        System.out.println("Waiting for containers: [" + sb.toString() + "] to reach Alive:"+alive);
        for (Container container : containers) {
            Future<Boolean> f = completionService.poll(timeout, TimeUnit.MILLISECONDS);
            if ( f == null || !f.get()) {
                throw new Exception("Container " + container.getId() + " failed to reach Alive:" + alive);
            }
        }
    }

    /**
     * Wait for all containers to become registered.
     * @param containers
     * @param alive
     * @param timeout
     * @throws Exception
     */
    public static void containersExist(Collection<String> containers, Long timeout) throws Exception {
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(EXECUTOR);
        List<Future<Boolean>> waitForProvisionTasks = new LinkedList<Future<Boolean>>();
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (String container : containers) {
            waitForProvisionTasks.add(completionService.submit(new WaitForContainerCreationTask(container, timeout)));
            sb.append(container).append(" ");
        }
        System.out.println("Waiting for containers: [" + sb.toString() + "] to become created.");
        for (String container : containers) {
            Future<Boolean> f = completionService.poll(timeout, TimeUnit.MILLISECONDS);
            if ( f == null || !f.get()) {
                throw new Exception("Container " + container + " failed to become created.");
            }
        }
    }

    /**
     * Wait for all containers to become alive.
     * @param containers
     * @param timeout
     * @throws Exception
     */
    public static void containerAlive(Collection<Container> containers, Long timeout) throws Exception {
        containersAlive(containers, true, timeout);
    }

    /**
     * Wait for a container to provision and assert its status.
     *
     * @param containers
     * @param timeout
     * @throws Exception
     */
    public static void provisioningSuccess(Collection<Container> containers, Long timeout) throws Exception {
        containerStatus(containers, timeout);
        for (Container container : containers) {
            if (!"success".equals(container.getProvisionStatus())) {
				throw new Exception("Container " + container.getId() + " failed to provision. Status:" + container.getProvisionStatus() + " Exception:" + container.getProvisionException());
			}
        }
    }

    public static Boolean profileAvailable(String profile, String version, Long timeout) throws Exception {
        FabricService service = ServiceLocator.getOsgiService(FabricService.class);
        for (long t = 0; (!service.getDataStore().hasProfile(version, profile)  && t < timeout); t += 2000L) {
            Thread.sleep(2000L);
        }
        return service.getDataStore().hasProfile(version, profile);
    }
}
