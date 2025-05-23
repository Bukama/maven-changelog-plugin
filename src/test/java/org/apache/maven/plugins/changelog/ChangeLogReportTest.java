/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.changelog;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.changelog.stubs.FailedScmManagerStub;
import org.apache.maven.plugins.changelog.stubs.ScmManagerStub;
import org.apache.maven.plugins.changelog.stubs.ScmManagerWithHostStub;
import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Edwin Punzalan
 */
public class ChangeLogReportTest extends AbstractChangeLogReportTest {
    private ScmManager scmManager = new ScmManagerStub();

    public void testNoSource() throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/plugin-configs/changelog/no-source-plugin-config.xml");

        ChangeLogReport mojo = (ChangeLogReport) lookupMojo("changelog", pluginXmlFile);

        assertNotNull("Mojo found.", mojo);

        this.setVariableValueToObject(mojo, "manager", scmManager);

        mojo.execute();

        File outputDir = (File) getVariableValueFromObject(mojo, "outputDirectory");

        File outputHtml = new File(outputDir, "changelog.html");

        renderer(mojo, outputHtml);

        assertTrue(outputHtml.getAbsolutePath() + " not generated!", outputHtml.exists());

        assertTrue(outputHtml.getAbsolutePath() + " is empty!", outputHtml.length() > 0);
    }

    public void testMinConfig() throws Exception {
        executeMojo("min-plugin-config.xml");
    }

    public void testFailedChangelog() throws Exception {
        scmManager = new FailedScmManagerStub();

        try {
            executeMojo("min-plugin-config.xml");
        } catch (MojoExecutionException e) {
            assertEquals(
                    "Test thrown exception",
                    "Command failed.",
                    e.getCause().getCause().getMessage());
        }
    }

    public void testUsageOfCachedXml() throws Exception {
        File cacheFile = new File(getBasedir(), "src/test/changelog-xml/min-changelog.xml");
        cacheFile.setLastModified(System.currentTimeMillis());

        executeMojo("cached-plugin-config.xml");
    }

    public void testTypeException() throws Exception {
        try {
            executeMojo("inv-type-plugin-config.xml");

            fail("Test exception on invalid type");
        } catch (MojoExecutionException e) {
            assertTrue(
                    "Test thrown exception",
                    e.getCause().getMessage().startsWith("The type parameter has an invalid value: invalid."));
        }
    }

    public void testTagType() throws Exception {
        executeMojo("tag-plugin-config.xml");
    }

    public void testTagsType() throws Exception {
        executeMojo("tags-plugin-config.xml");
    }

    public void testDateException() throws Exception {
        try {
            executeMojo("inv-date-plugin-config.xml");
        } catch (MojoExecutionException e) {
            assertTrue(
                    "Test thrown exception",
                    e.getCause().getCause().getMessage().startsWith("Please use this date pattern: "));
        }
    }

    public void testDateType() throws Exception {
        executeMojo("date-plugin-config.xml");
    }

    public void testDatesType() throws Exception {
        executeMojo("dates-plugin-config.xml");
    }

    public void testScmRepositoryWithHost() throws Exception {
        scmManager = new ScmManagerWithHostStub();

        executeMojo("hosted-plugin-config.xml");
    }

    public void testScmRepositoryWithHostFromSettings() throws Exception {
        scmManager = new ScmManagerWithHostStub();

        executeMojo("hosted-with-settings-plugin-config.xml");
    }

    public void testNoScmConnection() throws Exception {
        try {
            executeMojo("no-scm-plugin-config.xml");
        } catch (MojoExecutionException e) {
            assertEquals(
                    "Test thrown exception",
                    "SCM Connection is not set.",
                    e.getCause().getCause().getCause().getMessage());
        }
    }

    public void testDontDisplayFileAndRevInfo() throws Exception {
        File cacheFile = new File(getBasedir(), "src/test/changelog-xml/min-changelog.xml");
        cacheFile.setLastModified(System.currentTimeMillis());

        String html = executeMojo("dont-display-file-and-rev-info-plugin-config.xml", true);

        assertFalse(html.contains("file.extension"));
        assertFalse(html.contains("file2.extension"));
        assertFalse(html.contains(" v 1"));
        assertFalse(html.contains(" v 2"));
        assertFalse(html.contains(" v 3"));
        assertFalse(html.contains(" v 4"));
    }

    public void testDisplayFileAndRevInfoExplicitly() throws Exception {
        File cacheFile = new File(getBasedir(), "src/test/changelog-xml/min-changelog.xml");
        cacheFile.setLastModified(System.currentTimeMillis());

        String html = executeMojo("display-file-and-rev-info-explicitly-plugin-config.xml", true);

        assertTrue(html.contains("file.extension"));
        assertTrue(html.contains("file2.extension"));
        assertTrue(html.contains(" v 1"));
        assertTrue(html.contains(" v 2"));
        assertTrue(html.contains(" v 3"));
        assertTrue(html.contains(" v 4"));
    }

    private void executeMojo(String pluginXml) throws Exception {
        executeMojo(pluginXml, false);
    }

    private String executeMojo(String pluginXml, boolean withOutput) throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/plugin-configs/changelog/" + pluginXml);

        ChangeLogReport mojo = (ChangeLogReport) lookupMojo("changelog", pluginXmlFile);

        assertNotNull("Mojo found.", mojo);

        this.setVariableValueToObject(mojo, "manager", scmManager);

        mojo.execute();

        File outputXML = (File) getVariableValueFromObject(mojo, "outputXML");

        String encoding = (String) getVariableValueFromObject(mojo, "outputEncoding");

        assertTrue("Test if changelog.xml is created", outputXML.exists());

        String changelogXml = FileUtils.fileRead(outputXML);

        assertTrue(
                "Test for xml header",
                changelogXml.startsWith("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>"));

        assertTrue("Test for xml footer", changelogXml.endsWith("</changelog>"));

        File outputDir = (File) getVariableValueFromObject(mojo, "outputDirectory");

        File outputHtml = new File(outputDir, "changelog.html");

        renderer(mojo, outputHtml);

        assertTrue(outputHtml.getAbsolutePath() + " not generated!", outputHtml.exists());

        assertTrue(outputHtml.getAbsolutePath() + " is empty!", outputHtml.length() > 0);

        if (!withOutput) {
            return null;
        }

        return new String(Files.readAllBytes(outputHtml.toPath()));
    }
}
