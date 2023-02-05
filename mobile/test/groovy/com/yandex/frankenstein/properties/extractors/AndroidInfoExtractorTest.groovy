package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.AndroidInfo
import com.yandex.frankenstein.utils.SystemEnvironment
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.entry

class AndroidInfoExtractorTest {

    final String androidHome = "string_android_home"
    final String avdName = "string_avd_name"
    final String emulatorArguments = "string_emulator_arguments"
    final String qemuArguments = "string_qemu_arguments"
    final String emulatorEnvironment = "DISPLAY=:1.0 QEMU_AUDIO_DRV=none"
    final String buildType = "string_build_type"

    final Logger dummyLogger = [
            error: {}
    ] as Logger

    final Project dummyProject = [
            getProperties: {[:]},
            getLogger: { dummyLogger }
    ] as Project

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                    "android.home": androidHome,
                    "avd.name": avdName,
                    "emulator.args": emulatorArguments,
                    "qemu.args": qemuArguments,
                    "emulator.environment": emulatorEnvironment,
                    "buildType": buildType
                ]}
        ] as Project
        final AndroidInfoExtractor androidInfoExtractor = new AndroidInfoExtractor(null)
        final AndroidInfo androidInfo = androidInfoExtractor.extractInfo(project)
        assertThat(androidInfo.androidHome).isEqualTo(androidHome)
        assertThat(androidInfo.avdName).isEqualTo(avdName)
        assertThat(androidInfo.emulatorArguments).isEqualTo(emulatorArguments)
        assertThat(androidInfo.qemuArguments).isEqualTo(qemuArguments)
        assertThat(androidInfo.emulatorEnvironment)
                .containsExactly(entry('DISPLAY', ':1.0'), entry('QEMU_AUDIO_DRV', 'none'))
        assertThat(androidInfo.buildType).isEqualTo(buildType)
    }

    @Test
    void testGetAndroidHomeFromSystem() {
        final SystemEnvironment environment = [
                getAndroidHome: androidHome
        ] as SystemEnvironment
        final AndroidInfoExtractor androidInfoExtractor = new AndroidInfoExtractor(environment)
        final AndroidInfo androidInfo = androidInfoExtractor.extractInfo(dummyProject)
        assertThat(androidInfo.androidHome).isEqualTo(androidHome)
    }

    @Test
    void testGetAndroidHomeFromProperties() {
        final SystemEnvironment environment = [
                getAndroidHome: ""
        ] as SystemEnvironment
        final File agentDir = File.createTempDir()
        final File propertiesFile = new File(agentDir, "local.properties")
        propertiesFile.write("sdk.dir=" + androidHome)
        final AndroidInfoExtractor androidInfoExtractor = new AndroidInfoExtractor(environment)
        final AndroidInfo androidInfo = androidInfoExtractor.extractInfo(dummyProject, agentDir)
        assertThat(androidInfo.androidHome).isEqualTo(androidHome)
    }

    @Test
    void testGetAndroidHomeFromDefault() {
        final File localHome = File.createTempDir()
        final File localAndroidHome = new File(localHome, "Library/Android/sdk")
        localAndroidHome.mkdirs()
        final SystemEnvironment environment = [
                getAndroidHome: "",
                getHome: localHome.absolutePath
        ] as SystemEnvironment
        final AndroidInfoExtractor androidInfoExtractor = new AndroidInfoExtractor(environment)
        final AndroidInfo androidInfo = androidInfoExtractor.extractInfo(dummyProject)
        assertThat(androidInfo.androidHome).isEqualTo(localAndroidHome.absolutePath)
    }

    @Test
    void testGetAndroidHomeFromDefaultIfNoAndroidSdk() {
        final File localHome = File.createTempDir()
        final SystemEnvironment environment = [
                getAndroidHome: "",
                getHome: localHome.absolutePath
        ] as SystemEnvironment
        final AndroidInfoExtractor androidInfoExtractor = new AndroidInfoExtractor(environment)
        final AndroidInfo androidInfo = androidInfoExtractor.extractInfo(dummyProject)
        assertThat(androidInfo.androidHome).isEmpty()
    }
}
