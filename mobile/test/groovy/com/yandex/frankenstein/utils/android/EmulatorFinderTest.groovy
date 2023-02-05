package com.yandex.frankenstein.utils.android

import com.yandex.frankenstein.properties.info.AndroidInfo
import com.yandex.frankenstein.utils.CommandLineExecutor
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class EmulatorFinderTest {

    final String mAvdNamePattern = ".*555.*"

    final Map<String, String> mMatchingRunning = [
            'emulator-5554': 'emulator-5554-name',
    ]

    final Map<String, String> mNotMatchingRunning = [
            'emulator-5560': 'emulator-5560-name',
            'real-1': '',
    ]

    final Map<String, String> mMatching = [
            'emulator-5556': 'emulator-5556-name',
            'emulator-5558': 'emulator-5558-name',
    ]

    final Map<String, String> mNotMatching = [
            'emulator-5562': 'emulator-5562-name',
    ]

    final Logger mDummyLogger = [info: {}] as Logger

    final AndroidInfo mAndroidInfo = new AndroidInfo("", mAvdNamePattern, "", "", "", [:], "", "")

    final AdbProperties mAdbProperties = new AdbProperties(mDummyLogger, new AdbExecutor(mDummyLogger, "", "")) {
        List<String> getSerialNumbers() {
            return (mMatchingRunning + mNotMatchingRunning).keySet().toList()
        }
    }
    final CommandLineExecutor mCommandLineExecutor = new CommandLineExecutor(mDummyLogger) {
        CommandLineExecutor execute(final List<String> commandLine) {
            return this
        }

        String getOutput() {
            return (mMatching + mNotMatching + mMatchingRunning + mNotMatchingRunning).values().toList().join(System.lineSeparator())
        }
    }
    final EmulatorTelnetExecutor mEmulatorTelnetExecutor = new EmulatorTelnetExecutor(mDummyLogger) {
        String getRunningAvdName(final String serial) {
            return (mMatchingRunning + mNotMatchingRunning + mMatching + mNotMatching).get(serial)
        }
    }

    final EmulatorFinder mEmulatorFinder =
            new EmulatorFinder(mDummyLogger, mAndroidInfo, mAdbProperties, mEmulatorTelnetExecutor, mCommandLineExecutor)

    @Test
    void testFindSuitableEmulatorsSerials() {
        final List<String> suitableEmulators = mEmulatorFinder.findSuitableEmulatorsSerials()
        final List<String> expected = mMatchingRunning.keySet().toList()
        assertThat(suitableEmulators).containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    void testFindSuitableEmulatorsAvdNames() {
        final List<String> suitableEmulators = mEmulatorFinder.findSuitableEmulatorsAvdNames()
        final List<String> expected = mMatchingRunning.values().toList()
        assertThat(suitableEmulators).containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    void testGetMatchingAvdsNames() {
        final List<String> avdsNames = mEmulatorFinder.getMatchingAvdsNames()
        final List<String> expected = (mMatchingRunning + mMatching).values().toList()
        assertThat(avdsNames).containsExactlyInAnyOrderElementsOf(expected)

    }
}
