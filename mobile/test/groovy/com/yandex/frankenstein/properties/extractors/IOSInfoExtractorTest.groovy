package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.IOSInfo
import com.yandex.frankenstein.utils.CommandLineExecutor
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class IOSInfoExtractorTest {

    final String xcodePath = "/Applications/Xcode.app"
    final String xcodeVersion = "9.4.1"
    final String xcodeTestDevice = "iPhone"
    final String xcodeTestOs = "iOS8"
    final String buildType = "test build type"
    final String deviceType = "test device type"
    final String iosDeployPath = "test deploy path"
    final String simularotsCount = "10"

    final Logger dummyLogger = [
            info: {}
    ] as Logger

    final Project dummyProject = [
            getProperties: {[:]},
            getLogger: { dummyLogger },
    ] as Project

    final Project fullProject = [
            getProperties: {[
                    "xcodePath": xcodePath,
                    "xcodeVersion": xcodeVersion,
                    "xcodeTestDevice": xcodeTestDevice,
                    "xcodeTestOS": xcodeTestOs,
                    "buildType": buildType,
                    "deviceType": deviceType,
                    "ios_deploy.path": iosDeployPath,
                    "environment.maxParallelForks": simularotsCount
            ]},
            getLogger: { dummyLogger }
    ] as Project

    final Project versionProject = [
            getProperties: {[
                    "xcodeVersion": xcodeVersion
            ]},
            getLogger: { dummyLogger }
    ] as Project

    final IOSInfoExtractor mIOSInfoExtractor = new IOSInfoExtractor()

    @Test
    void testExtractInfo() {
        final IOSInfo iosInfo = mIOSInfoExtractor.extractInfo(fullProject)
        assertThat(iosInfo.xcodePath).isEqualTo(xcodePath)
        assertThat(iosInfo.deviceModel).isEqualTo(xcodeTestDevice)
        assertThat(iosInfo.osVersion).isEqualTo(xcodeTestOs)
        assertThat(iosInfo.developerDir).isEqualTo(xcodePath + "/Contents/Developer")
        assertThat(iosInfo.deviceType).isEqualTo(deviceType)
        assertThat(iosInfo.buildType).isEqualTo(buildType)
        assertThat(iosInfo.iosDeployPath).isEqualTo(iosDeployPath)
        assertThat(iosInfo.simulatorsCount).isEqualTo(simularotsCount)
    }

    @Test
    void testStaticGetters() {
        assertThat(IOSInfoExtractor.getXcodePath(fullProject)).isEqualTo(xcodePath)
        assertThat(IOSInfoExtractor.getXcodeVersion(fullProject)).isEqualTo(xcodeVersion)
        assertThat(IOSInfoExtractor.getDeviceModel(fullProject)).isEqualTo(xcodeTestDevice)
        assertThat(IOSInfoExtractor.getOsVersion(fullProject)).isEqualTo(xcodeTestOs)
        assertThat(IOSInfoExtractor.getDeviceType(fullProject)).isEqualTo(deviceType)
        assertThat(IOSInfoExtractor.getBuildType(fullProject)).isEqualTo(buildType)
        assertThat(IOSInfoExtractor.getIOSDeployPath(fullProject)).isEqualTo(iosDeployPath)
        assertThat(IOSInfoExtractor.getSimulatorsCount(fullProject)).isEqualTo(simularotsCount)
    }

    @Test
    void testFindXcodePath() {
        final Project project = [
                getProperties: {[
                        "xcodePath": xcodePath
                ]},
                getLogger: { dummyLogger },
        ] as Project
        final String xcodePath = mIOSInfoExtractor.findXcodePath(project, null)
        assertThat(xcodePath).isEqualTo(this.xcodePath)
    }

    @Test
    void testFindXcodePathWithoutPath() {
        final File xcode = File.createTempDir()
        final File xcodeBuild = new File(xcode, "/Contents/Developer/usr/bin/xcodebuild")
        xcodeBuild.mkdirs()
        final CommandLineExecutor executor = new CommandLineExecutor(dummyLogger) {
            private String output = ""

            CommandLineExecutor execute(final List<String> commandLine) {
                if (commandLine.containsAll(['mdfind', 'kMDItemCFBundleIdentifier=com.apple.dt.Xcode'])) {
                    output = xcode.absolutePath
                } else if (commandLine.containsAll([xcodeBuild.absolutePath, '-version'])) {
                    output = "Xcode " + xcodeVersion + "\n" +
                            "Build version 9F2000"
                }
                return this
            }

            String getOutput() {
                return output
            }
        }
        final String extractedXcodePath = mIOSInfoExtractor.findXcodePath(versionProject, executor)
        assertThat(extractedXcodePath).isEqualTo(xcode.absolutePath)
    }

    @Test
    void testSortInFindXcodePathWithoutPath() {
        final File oldXcode = File.createTempDir()
        final File oldXcodeBuild = new File(oldXcode, "/Contents/Developer/usr/bin/xcodebuild")
        oldXcodeBuild.mkdirs()
        final String oldXcodeVersion = "1.1.1"

        final File newXcode = File.createTempDir()
        final File newXcodeBuild = new File(newXcode, "/Contents/Developer/usr/bin/xcodebuild")
        newXcodeBuild.mkdirs()
        final String newXcodeVersion = "2.2.2"

        final CommandLineExecutor executor = new CommandLineExecutor(dummyLogger) {
            private String output = ""

            CommandLineExecutor execute(final List<String> commandLine) {
                if (commandLine.containsAll(['mdfind', 'kMDItemCFBundleIdentifier=com.apple.dt.Xcode'])) {
                    output = oldXcode.absolutePath + "\n" + newXcode.absolutePath
                } else if (commandLine.containsAll([oldXcodeBuild.absolutePath, '-version'])) {
                    output = "Xcode " + oldXcodeVersion + "\n" +
                            "Build version 9F2000"
                } else if (commandLine.containsAll([newXcodeBuild.absolutePath, '-version'])) {
                    output = "Xcode " + newXcodeVersion + "\n" +
                            "Build version 9F2000"
                } else if (commandLine.containsAll(['xcode-select', '-p'])) {
                    output = ""
                }
                return this
            }

            String getOutput() {
                return output
            }
        }
        final String extractedXcodePath = mIOSInfoExtractor.findXcodePath(dummyProject, executor)
        assertThat(extractedXcodePath).isEqualTo(newXcode.absolutePath)
    }

    @Test
    void testFindXcodePathWithoutPathAndXcodeBuildPath() {
        final String xcodePath = File.createTempDir().absolutePath
        final CommandLineExecutor executor = new CommandLineExecutor(dummyLogger) {
            private String output = ""

            CommandLineExecutor execute(final List<String> commandLine) {
                if (commandLine.containsAll(['mdfind', 'kMDItemCFBundleIdentifier=com.apple.dt.Xcode'])) {
                    output = xcodePath
                } else {
                    output = super.execute(commandLine).getOutput()
                }
                return this
            }

            String getOutput() {
                return output
            }
        }
        final String extractedXcodePath = mIOSInfoExtractor.findXcodePath(versionProject, executor)
        assertThat(extractedXcodePath).isEqualTo(null)
    }

    @Test
    void testFindXcodePathWithoutPathAndVersion() {
        final CommandLineExecutor executor = new CommandLineExecutor(dummyLogger) {
            private String output = ""

            CommandLineExecutor execute(final List<String> commandLine) {
                if (commandLine.containsAll(['xcode-select', '-p'])) {
                    output = xcodePath
                }
                return this
            }

            String getOutput() {
                return output
            }
        }
        final String extractedXcodePath = mIOSInfoExtractor.findXcodePath(dummyProject, executor)
        assertThat(extractedXcodePath).isEqualTo(xcodePath)
    }
}
