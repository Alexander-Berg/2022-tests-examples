package com.yandex.frankenstein.utils.ios


import com.yandex.frankenstein.properties.info.IOSInfo
import com.yandex.frankenstein.utils.CommandLineExecutor
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class SimulatorBooterTest {

    final List<String> mUdids = ['5F44CB87-02E0-44A6-B262-47286F9A599D', '5L96IB87-JF68-4B20-BCD9-3DCK0D7E45E2']
    final Logger mDummyLogger = [info: {}] as Logger

    final List<String> mStartedSimulators = []
    final List<String> mErasedSimulators = []

    final IOSInfo mIosInfo = new IOSInfo("", "", "", "", "", "", "")

    final CommandLineExecutor mExecutor = new CommandLineExecutor(mDummyLogger) {
        @Override
        CommandLineExecutor execute(final List<String> commandLine) {
            if (commandLine[2] == 'boot') {
                mStartedSimulators.add(commandLine[3])
            }
            if (commandLine[2] == 'erase') {
                mErasedSimulators.add(commandLine[3])
            }
            return this
        }
    }

    final SimctlDevices mSimctlDevices = new SimctlDevices([:]) {
        @Override
        List<String> getAllBootedDevicesUdids() {
            return mStartedSimulators
        }
    }

    final SimctlList mSimctlList = [
            'update': {},
            'getDevices': { mSimctlDevices }
    ] as SimctlList

    final SimulatorBooter mSimulatorBooter = new SimulatorBooter(mDummyLogger, mIosInfo, mExecutor, mSimctlList)

    @Test
    void testBootSimulators() {
        final List<String> bootedSimulators = mSimulatorBooter.bootSimulators(mUdids)
        assertThat(bootedSimulators).containsExactlyInAnyOrderElementsOf(mUdids)
        assertThat(mStartedSimulators).containsExactlyInAnyOrderElementsOf(mUdids)
        assertThat(mErasedSimulators).containsExactlyInAnyOrderElementsOf(mUdids)
    }
}
