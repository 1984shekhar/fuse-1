/*
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
package io.fabric8.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import io.fabric8.api.FabricService;
import io.fabric8.api.PatchException;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class PatchServiceImplTest {

    @Test
    public void testPatchDescriptorWithoutDirectives() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("test1.patch"));
        PatchServiceImpl.PatchDescriptor descriptor = new PatchServiceImpl.PatchDescriptor(properties);
        assertEquals(2, descriptor.getBundles().size());
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test1/1.2.0"));
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test2/1.2.0"));
    }

    @Test
    public void testPatchDescriptorWithDirectives() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("test2.patch"));
        PatchServiceImpl.PatchDescriptor descriptor = new PatchServiceImpl.PatchDescriptor(properties);
        assertEquals(2, descriptor.getBundles().size());
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test1/1.2.0;range=[1.0.0,2.0.0)"));
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test2/1.2.0"));
    }

    @Test
    public void testFailOnCorruptedZip() throws URISyntaxException, MalformedURLException {
        FabricService mockFabricService = Mockito.mock(FabricService.class);
        when(mockFabricService.getMavenRepoUploadURI()).thenReturn(new URI("http://dummy"));
        PatchServiceImpl patchService = new PatchServiceImpl(mockFabricService);

        Version version =  Mockito.mock(Version.class);

        URL url = getClass().getClassLoader().getResource("corrupted_archive.zip");


        try {
            patchService.applyPatch(version, url, "not_relevant", "not_relevant");
            fail("Expected PatchException has not been triggered.");
        } catch (PatchException e){
            assertNotNull(e);
            e.printStackTrace();
        } catch (Exception e){
            fail("Note the expected exception: " + e);
            e.printStackTrace();
        }
    }
}
