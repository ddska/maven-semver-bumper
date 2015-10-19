#!/usr/bin/env groovy
@Grab('com.github.zafarkhaja:java-semver:0.9.0')
import com.github.zafarkhaja.semver.Version

import java.nio.file.Path
import java.nio.file.Paths

public class Bumper {
    public static void main(String[] args) {
        println "Script to bump semantic versions before branching"
        if (args == null || args.length == 0) {
            println "pass path to root maven pom file to bump all it's sub-modules versions"
        }

        println "Root pom: " + args[0]

        processProject(args[0])
    }

    static String processProject(String pomPath) {
        println "==========================="
        Path xmlFilePath = Paths
                .get(pomPath)
                .toAbsolutePath()
        println xmlFilePath
        Node root = new XmlParser().parse(xmlFilePath.toFile())

        assert "project" == root.name().getLocalPart()
        bumpVersion(pomPath)
        if (isProjectPomPackaging(root)) {
            def modules = root.modules.module
            int modulesCount = modules.size()
            for (int i = 0; i < modulesCount; i++) {
                String moduleName = modules[i].value().text()
                String modulePomPath = xmlFilePath.toAbsolutePath().getParent().toAbsolutePath().toString() +
                        File.separatorChar + moduleName + File.separatorChar + "pom.xml"
                processProject(modulePomPath)
            }
        }
    }

    static boolean isProjectPomPackaging(Node root) {
        return "pom" == root.packaging.text()
    }

    static boolean bumpVersion(String pomPath) {
        String command = "mvn help:evaluate -f " + pomPath +
                " -Dexpression=project.version" +
                " |grep -Ev '(^\\[|Download\\w+:)' "
        println "Executing " + command
        def process = ["bash", "-c", command].execute()
        int exitCode = process.waitFor()
        def version = process.text.trim()
        println "Current ver: " + version

        try {
            Version ver = Version.valueOf(version);
            String preReleaseVersion = ver.getPreReleaseVersion();
            Version nextVersion = ver.incrementMajorVersion();
            if (preReleaseVersion != null && !preReleaseVersion.isEmpty()) {
                nextVersion = nextVersion.setPreReleaseVersion(preReleaseVersion);
            }
            println "Next ver:" + nextVersion;
            command = "mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -f " + pomPath +
                    " -DnewVersion=" + nextVersion
            println "Executing " + command
            ["bash", "-c", command].execute().waitFor();
        }
        catch (Exception e) {
            println "Error parsing version " + version;
            e.printStackTrace();
        }


    }

}
