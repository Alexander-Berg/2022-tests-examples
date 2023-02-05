package com.yandex.frankenstein.utils.ios

import com.yandex.frankenstein.TestDevicesInfo
import com.yandex.frankenstein.properties.info.IOSInfo
import com.yandex.frankenstein.utils.CommandLineExecutor
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class PhysicalIOSDeviceControllerTest {

    final List<String> mIds = ['some_random_device_1', 'some_random_device_2', 'some_random_device_3']

    final Logger mDummyLogger = [info: {}] as Logger
    final IOSInfo mIosInfo = new IOSInfo("", "", "", "", "", "ios_deploy_path", "")

    final CommandLineExecutor mCommandLineExecutor = new CommandLineExecutor(mDummyLogger) {
        CommandLineExecutor execute(final List<String> commandLine) {
            return this
        }

        String getOutput() {
            return "Waiting up to 5 seconds for iOS device to be connected\n" +
                    mIds.collect { "[....] Found $it connected through USB." }.join("\n")
        }
    }
    final PhysicalIOSDeviceController mDeviceController =
            new PhysicalIOSDeviceController(mDummyLogger,mIosInfo, mCommandLineExecutor)

    @Test
    void testFindDevices() {
        final int count = 2
        final TestDevicesInfo info = mDeviceController.findDevices(count)

        assertThat(info.running).hasSize(count).isSubsetOf(mIds)
        assertThat(info.booted).isEmpty()
    }

    @Test
    void testFindAllDevices() {
        final List<String> suitableEmulators = mDeviceController.findAllDevices()
        assertThat(suitableEmulators).containsExactlyInAnyOrderElementsOf(mIds)
    }
}
