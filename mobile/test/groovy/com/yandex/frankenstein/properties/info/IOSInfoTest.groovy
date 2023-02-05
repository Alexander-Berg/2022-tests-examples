package com.yandex.frankenstein.properties.info

import com.yandex.frankenstein.DeviceSettings
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class IOSInfoTest {

    final String xcodePath = "/Applications/Xcode.app"
    final String xcodeTestDevice = "iPhone"
    final String xcodeTestOs = "iOS8"
    final String buildType = "test build type"
    final String deviceType = "test device type"
    final String iosDeployPath = "test deploy path"
    final String simulatorsCount = "10"

    @Test
    void testExtractInfo() {
        final IOSInfo iosInfo = new IOSInfo(xcodePath, xcodeTestDevice, xcodeTestOs,
                deviceType, buildType, iosDeployPath, simulatorsCount)
        assertThat(iosInfo.xcodePath).isEqualTo(xcodePath)
        assertThat(iosInfo.deviceModel).isEqualTo(xcodeTestDevice)
        assertThat(iosInfo.osVersion).isEqualTo(xcodeTestOs)
        assertThat(iosInfo.developerDir).isEqualTo(xcodePath + "/Contents/Developer")
        assertThat(iosInfo.deviceType).isEqualTo(deviceType)
        assertThat(iosInfo.buildType).isEqualTo(buildType)
        assertThat(iosInfo.iosDeployPath).isEqualTo(iosDeployPath)
        assertThat(iosInfo.simulatorsCount).isEqualTo(simulatorsCount)
    }

    @Test
    void testExtractInfoWithoutParameters() {
        final IOSInfo iosInfo = new IOSInfo(null, null, null,
                null, null, null, null)
        assertThat(iosInfo.xcodePath).isNull()
        assertThat(iosInfo.deviceModel).isEqualTo('iPhone .*')
        assertThat(iosInfo.osVersion).isEqualTo('11\\..*')
        assertThat(iosInfo.developerDir).isNull()
        assertThat(iosInfo.deviceType).isEqualTo(DeviceSettings.DeviceType.VIRTUAL)
        assertThat(iosInfo.buildType).isEqualTo(LibraryBuildType.BINARY)
        assertThat(iosInfo.iosDeployPath).isEqualTo('ios-deploy')
        assertThat(iosInfo.simulatorsCount).isEqualTo('1')
    }

    @Test
    void testIsDeviceTypePhysical() {
        final IOSInfo iosInfo = new IOSInfo(null, null, null,
                DeviceSettings.DeviceType.PHYSICAL, null, null, null)
        assertThat(iosInfo.isDeviceTypePhysical()).isTrue()
    }

    @Test
    void testIsBuildTypeBinary() {
        final IOSInfo iosInfo = new IOSInfo(null, null, null,
                null, LibraryBuildType.BINARY, null, null)
        assertThat(iosInfo.isBuildTypeBinary()).isTrue()
    }
}
