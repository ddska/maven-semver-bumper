#!/usr/bin/env groovy
@Grab('com.github.zafarkhaja:java-semver:0.9.0')
@Grab('com.ximpleware:vtd-xml:2.11')

import com.github.zafarkhaja.semver.Version
import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import com.ximpleware.XMLModifier

import java.nio.file.Path
import java.nio.file.Paths

public class Bumper {

    private static String parentPomPath

    public static void main(String[] args) {
        println "Script to bump semantic versions before branching"
        println "Requirements: Maven 3.3+"
        if (args == null || args.length == 0) {
            println "pass 1) path to root maven pom file to bump all it's sub-modules versions"
            println "2) path to parent pom file which contains dependency management to set versions of bumped modules"
            println "In parent pom file module versions must be in <artifactId>.version format"
            println "e.g if you have my-api artifact then you should name property \"my-api.version\""
        }

        println "Root pom: " + args[0]
        println "parent pom: " + args[1]
        parentPomPath = args[1]
        processProject(args[0], "")
    }

    private static void processProject(String pomPath, String parentVersion) {
        println "==========================="
        Path xmlFilePath = Paths
                .get(pomPath)
                .toAbsolutePath()
        println "Processing POM: " + xmlFilePath
        Node root = new XmlParser().parse(xmlFilePath.toFile())

        assert "project" == root.name().getLocalPart()
        String rootModuleName = root.artifactId[0].text()
        println "Module name: " + rootModuleName

        String newVersion = bumpVersion(pomPath, parentVersion)

        setParentPomDepsVersion(rootModuleName, newVersion)
        if (isProjectPomPackaging(root)) {
            def modules = root.modules.module
            int modulesCount = modules.size()
            for (int i = 0; i < modulesCount; i++) {
                String moduleName = modules[i].value().text()
                String modulePomPath = xmlFilePath.toAbsolutePath().getParent().toAbsolutePath().toString() +
                        File.separatorChar + moduleName + File.separatorChar + "pom.xml"
                processProject(modulePomPath, newVersion)
            }
        }
    }

    private static void setParentPomDepsVersion(String rootModuleName, String newVersion) {
        println "Set parent pom property version for " + rootModuleName + " with " + newVersion

        VTDGen vg = new VTDGen()
        XMLModifier xm = new XMLModifier()
        if (vg.parseFile(parentPomPath, false))
        {
            VTDNav vn = vg.getNav()
            xm.bind(vn)

            if (vn.toElement(VTDNav.FIRST_CHILD, "properties"))
            {
                if (vn.toElement(VTDNav.FIRST_CHILD, rootModuleName + ".version"))
                {
                    int i = vn.getText()
                    if (i != -1)
                    {
                        xm.updateToken(i, newVersion)
                    }
                }
            }

            xm.output(new FileOutputStream(parentPomPath))
        }
    }

    private static boolean isProjectPomPackaging(Node root) {
        return "pom" == root.packaging.text()
    }

    private static String getProjectVersion(String pomPath) {
        String command = "mvn help:evaluate -f " + pomPath +
                " -Dexpression=project.version -o"
        println "Executing: " + command
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command)

        Process process = builder.start()

        InputStream stdout = process.getInputStream ()

        BufferedReader reader = new BufferedReader (new InputStreamReader(stdout))

        String line
        while ((line = reader.readLine ()) != null) {
            if (!line.startsWith("[") && !line.startsWith("Download")) {
                println "Current ver: " + line
                return line
            }
        }
        throw new RuntimeException("Can't determine version")
    }

    private static String getProjectBumpedVersion(String oldVersion) {
        try {
            println "Trying to bump version " + oldVersion
            Version ver = Version.valueOf(oldVersion)
            String preReleaseVersion = ver.getPreReleaseVersion()
            Version nextVersion = ver.incrementMajorVersion()
            if (preReleaseVersion != null && !preReleaseVersion.isEmpty()) {
                nextVersion = nextVersion.setPreReleaseVersion(preReleaseVersion)
            }
            println "Next ver:" + nextVersion
            return nextVersion.toString()
        }
        catch (Exception e) {
            println "Error parsing version " + oldVersion;
            e.printStackTrace();
        }
    }

    private static String bumpVersion(String pomPath, String parentVersion) {
        String currentVersion = getProjectVersion(pomPath)

        if (currentVersion.equals(parentVersion)) {
            return parentVersion
        }
        String nextVersion = getProjectBumpedVersion(currentVersion)
        String command = "mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -f " + pomPath +
                " -DgenerateBackupPoms=false -DnewVersion=" + nextVersion
        println "Executing: " + command
        int exitCode = ["bash", "-c", command].execute().waitFor()
        // TODO: check exit code
        return nextVersion
    }

}
