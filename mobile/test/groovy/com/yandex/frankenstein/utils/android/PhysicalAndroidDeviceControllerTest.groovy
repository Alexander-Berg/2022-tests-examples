package com.yandex.frankenstein.utils.android

import com.yandex.frankenstein.TestDevicesInfo
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class PhysicalAndroidDeviceControllerTest {

    final List<String> mMatchingSerials = ['some_random_device_1', 'some_random_device_2']
    final List<String> mNotMatchingSerials = ['emulator-5556', 'emulator-5558']
    final List<String> mAllSerials = mMatchingSerials + mNotMatchingSerials

    final Logger mDummyLogger = [info: {}] as Logger

    final AdbProperties mAdbProperties = new AdbProperties(mDummyLogger, new AdbExecutor(mDummyLogger, "", "")) {
        List<String> getSerialNumbers() {
            return mAllSerials
        }
    }
    final EmulatorTelnetExecutor mEmulatorTelnetExecutor = new EmulatorTelnetExecutor(mDummyLogger) {
        String getRunningAvdName(final String serial) {
            return serialToName(serial)
        }
    }
    final PhysicalAndroidDeviceController mDeviceController =
            new PhysicalAndroidDeviceController(mDummyLogger, mAdbProperties, mEmulatorTelnetExecutor)

    @Test
    void testFindDevices() {
        final int count = 2
        final TestDevicesInfo info = mDeviceController.findDevices(count)

        assertThat(info.running).hasSize(count).isSubsetOf(mMatchingSerials)
        assertThat(info.booted).isEmpty()
    }

    @Test
    void testFindSuitableEmulators() {
        final List<String> suitableEmulators = mDeviceController.findSuitableDevices(mAllSerials)
        assertThat(suitableEmulators).containsExactlyInAnyOrderElementsOf(mMatchingSerials)
    }

    private static String serialToName(final String serial) {
        return serial.contains('-') ? "$serial-name".toString() : ''
    }
}
