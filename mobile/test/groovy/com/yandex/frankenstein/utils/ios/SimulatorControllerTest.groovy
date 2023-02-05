package com.yandex.frankenstein.utils.ios

import com.yandex.frankenstein.TestDevicesInfo
import com.yandex.frankenstein.properties.info.IOSInfo
import com.yandex.frankenstein.utils.CommandLineExecutor
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class SimulatorControllerTest {

    final String mDeviceModelPattern = ".*6s.*"
    final String mOsVersionPattern = "11.*"
    final List<String> mMatchingUdids = ['5F44CB87-02E0-44A6-B262-47286F9A599D', '5L96IB87-JF68-4B20-BCD9-3DCK0D7E45E2']
    final List<String> mNotMatchingUdids = ['75A2A3F7-B558-4638-A54D-1DF47EE0CD18', '7684AC37-771C-4F0E-BF93-1E840198F829']
    final Logger mDummyLogger = [info: {}] as Logger

    final IOSInfo mIosInfo = new IOSInfo("", mDeviceModelPattern, mOsVersionPattern, "", "", "", "")

    final String mSimctlFirstOutput = SimulatorBooterTest.classLoader.getResourceAsStream("simctl_devices.json").text
    final Map mSimctlJson = new JsonSlurper().parseText(mSimctlFirstOutput) as Map

    final CommandLineExecutor mExecutor = new CommandLineExecutor(mDummyLogger) {
        @Override
        CommandLineExecutor execute(final List<String> commandLine) {
            if (commandLine[2] == 'boot') {
                makeDeviceBooted(commandLine[3])
            }
            return this
        }

        @Override
        String getOutput() {
            return new JsonBuilder(mSimctlJson).toPrettyString()
        }
    }

    final SimctlList mSimctlList = new SimctlList()
    final SimulatorBooter mSimulatorBooter = new SimulatorBooter(mDummyLogger, mIosInfo, mExecutor, mSimctlList)

    final SimulatorController mSimulatorController =
            new SimulatorController(mDummyLogger, mIosInfo, mExecutor, mExecutor, mSimctlList, mSimulatorBooter)

    @Test
    void testGetMatchingSimulatorRuntime() {
        final Map<String, String> matchingRuntime = mSimulatorController.getMatchingSimulatorRuntime()

        assertThat(matchingRuntime)
                .containsEntry('name', 'iOS 11.0')
                .containsEntry('version', '11.0')
    }

    @Test
    void testGetMatchingSimulatorType() {
        final Map<String, String> matchingSimulatorType = mSimulatorController.getMatchingSimulatorType()

        assertThat(matchingSimulatorType['name']).matches(mDeviceModelPattern)
    }

    @Test
    void testGetMatchingSimulators() {
        final String deviceTypeName = 'iPhone 6s'

        final List<Map<String, String>> simulators = mSimulatorController.getMatchingSimulators(
                [name: 'iOS 11.0'],
                [name: deviceTypeName])


        assertThat(simulators)
                .hasSize(2)
                .extracting('name')
                .containsOnly(deviceTypeName)

        assertThat(simulators)
                .extracting('udid')
                .containsExactlyInAnyOrderElementsOf(mMatchingUdids)
    }

    @Test
    void testLaunchOneSimulator() {

        final TestDevicesInfo testDevicesInfo = mSimulatorController.bootSimulators(1)
        assertThat(testDevicesInfo.running).isEmpty()
        assertThat(testDevicesInfo.booted)
                .containsAnyElementsOf(mMatchingUdids)
                .hasSize(1)
    }

    @Test
    void testLaunchTwoSimulators() {
        final TestDevicesInfo testDevicesInfo = mSimulatorController.bootSimulators(2)
        assertThat(testDevicesInfo.running).isEmpty()
        assertThat(testDevicesInfo.booted).containsExactlyInAnyOrderElementsOf(mMatchingUdids)
    }

    @Test
    void testLaunchTwoSimulatorWithOneSuitableRunning() {
        makeDeviceBooted(mMatchingUdids[0])
        final SimulatorController simulatorController =
                new SimulatorController(mDummyLogger, mIosInfo, mExecutor, mExecutor, mSimctlList, mSimulatorBooter)
        final TestDevicesInfo testDevicesInfo = simulatorController.bootSimulators(2)
        assertThat(testDevicesInfo.running).containsExactly(mMatchingUdids[0])
        assertThat(testDevicesInfo.booted).containsExactly(mMatchingUdids[1])
    }

    @Test
    void testLaunchTwoSimulatorWithOneNotSuitableRunning() {
        makeDeviceBooted(mNotMatchingUdids[0])
        final SimulatorController simulatorController =
                new SimulatorController(mDummyLogger, mIosInfo, mExecutor, mExecutor, mSimctlList, mSimulatorBooter)
        final TestDevicesInfo testDevicesInfo = simulatorController.bootSimulators(2)
        assertThat(testDevicesInfo.running).isEmpty()
        assertThat(testDevicesInfo.booted).containsExactlyElementsOf(mMatchingUdids)
    }

    @CompileDynamic
    private void makeDeviceBooted(final String udid) {
        mSimctlJson['devices'].each { final String runtime, final List<Map> deviceList ->
            deviceList.findAll { it.udid == udid }
                    .each { it.state = 'Booted' }
        }
    }
}
