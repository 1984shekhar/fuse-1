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
package org.fusesource.fabric.openshift;

import org.eclipse.jgit.api.Git;
import org.fusesource.common.util.XPathBuilder;
import org.fusesource.common.util.XPathFacade;
import org.fusesource.fabric.agent.mvn.Parser;
import org.fusesource.fabric.agent.utils.XmlUtils;
import org.fusesource.fabric.openshift.agent.OpenShiftPomDeployer;
import org.fusesource.fabric.utils.Files;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class OpenShiftPomDeployerTest {
    protected static XPathBuilder xpathBuilder = new XPathBuilder();

    protected File baseDir;
    protected Git git;
    protected String deployDir = "shared";
    protected String webAppDir = "webapps";
    protected String[] artifactUrls = {
        "mvn:io.hawt/hawtio-web/1.2-M10/war",
        "mvn:org.apache.camel/camel-core/2.12.0"
    };


    @Before
    public void init() {
        baseDir = new File(System.getProperty("basedir", "."));
        assertDirectoryExists(baseDir);
    }

    @Test
    public void testPomWithNoOpenShiftProfile() throws Exception {
        doTest("noOpenShiftProfile", artifactUrls);
    }

    @Test
    public void testUpdate() throws Exception {
        doTest("update", artifactUrls);
    }


    protected void doTest(String folder, String[] artifactUrls) throws Exception {
        File sourceDir = new File(baseDir, "src/test/resources/" + folder);
        assertDirectoryExists(sourceDir);
        File pomSource = new File(sourceDir, "pom.xml");
        assertFileExists(pomSource);

        File outputDir = new File(baseDir, "target/" + getClass().getName() + "/" + folder);
        outputDir.mkdirs();
        assertDirectoryExists(outputDir);
        File pom = new File(outputDir, "pom.xml");
        Files.copy(pomSource, pom);
        assertFileExists(pom);

        git = Git.init().setDirectory(outputDir).call();
        assertDirectoryExists(new File(outputDir, ".git"));

        git.add().addFilepattern("pom.xml").call();
        git.commit().setMessage("Initial import").call();

        // now we have the git repo setup; lets run the update
        OpenShiftPomDeployer deployer = new OpenShiftPomDeployer(git, outputDir, deployDir, webAppDir);
        System.out.println("About to update the pom " + pom + " with artifacts: " + Arrays.asList(artifactUrls));

        List<Parser> artifacts = new ArrayList<Parser>();
        for (String artifactUrl : artifactUrls) {
            artifacts.add(new Parser(artifactUrl));
        }
        deployer.update(artifacts);

        System.out.println("Completed the new pom is: ");
        System.out.println(Files.toString(pom));

        Document xml = XmlUtils.parseDoc(pom);
        Element plugins = assertXPathElement(xml, "project/profiles/profile[id = 'openshift']/build/plugins");

        Element cleanExecution = assertXPathElement(plugins, "plugin[artifactId = 'maven-clean-plugin']/executions/execution[id = 'fuse-fabric-clean']");

        Element dependencySharedExecution = assertXPathElement(plugins, "plugin[artifactId = 'maven-dependency-plugin']/executions/execution[id = 'fuse-fabric-deploy-shared']");

        Element dependencyWebAppsExecution = assertXPathElement(plugins, "plugin[artifactId = 'maven-dependency-plugin']/executions/execution[id = 'fuse-fabric-deploy-webapps']");

        Element warPluginWarName = xpath( "plugin[artifactId = 'maven-war-plugin']/configuration/warName").element(plugins);
        if (warPluginWarName != null) {
            String warName = warPluginWarName.getTextContent();
            System.out.println("WarName is now:  " + warName);
            assertTrue("Should not have ROOT war name", !"ROOT".equals(warName));
        }
    }

    public static Element assertXPathElement(Node xml, String xpathExpression) throws XPathExpressionException {
        Element element = xpath(xpathExpression).element(xml);
        assertNotNull("Should have found element for XPath " + xpathExpression + " on " + xml, element);
        return element;
    }

    public static void assertFileExists(File file) {
        assertTrue("File " + file + " does not exist!", file.exists());
    }

    public static void assertDirectoryExists(File file) {
        assertFileExists(file);
        assertTrue("File " + file + " is not a directory!", file.isDirectory());
    }


    public static XPathFacade xpath(String expression) throws XPathExpressionException {
        return xpathBuilder.create(expression);
    }

}
