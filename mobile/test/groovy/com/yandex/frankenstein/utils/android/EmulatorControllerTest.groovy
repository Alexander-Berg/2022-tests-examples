package com.yandex.frankenstein.utils.android

import com.yandex.frankenstein.TestDevicesInfo
import com.yandex.frankenstein.properties.info.AndroidInfo
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.properties.info.StatfaceInfo
import com.yandex.frankenstein.utils.CommandLineExecutor
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class EmulatorControllerTest {

    final Map<String, String> mRunningSuitableEmulators = [
            'emulator-5554-name': 'emulator-5554',
            'emulator-5556-name': 'emulator-5556',
    ]

    final Map<String, String> mSuitableEmulators = [
            'emulator-5558-name': 'emulator-5558',
            'emulator-5560-name': 'emulator-5560',
            'emulator-5562-name': 'emulator-5562',
            'emulator-5564-name': 'emulator-5564',
    ]

    final Logger mDummyLogger = [info: {}] as Logger
    final AndroidInfo mDummyAndroidInfo = new AndroidInfo("", "", "", "", "", [:], "", "")
    final BuildInfo mDummyBuildInfo = new BuildInfo("", "", "", "", "", "")
    final StatfaceInfo mDummyStatfaceInfo = new StatfaceInfo("", "", "")
    final File mEmulatorLogsDir = File.createTempDir()
    final AdbProperties mAdbProperties = new AdbProperties(mDummyLogger, new AdbExecutor(mDummyLogger, "", ""))
    final EmulatorTelnetExecutor mEmulatorTelnetExecutor = new EmulatorTelnetExecutor(mDummyLogger)
    final CommandLineExecutor mCommandLineExecutor = new CommandLineExecutor(mDummyLogger)

    final EmulatorFinder mEmulatorFinder = new EmulatorFinder(mDummyLogger, mDummyAndroidInfo, mAdbProperties,
            mEmulatorTelnetExecutor, mCommandLineExecutor) {
        List<String> findSuitableEmulatorsSerials() {
            return mRunningSuitableEmulators.values().toList()
        }

        List<String> findSuitableEmulatorsAvdNames() {
            return mRunningSuitableEmulators.keySet().toList()
        }

        List<String> getMatchingAvdsNames() {
            return mSuitableEmulators.keySet().toList()
        }
    }
    final EmulatorBooter mEmulatorBooter = new EmulatorBooter(mDummyLogger, mDummyAndroidInfo, mEmulatorLogsDir) {
        String bootEmulator(final String avdNameForBoot, final String name) {
            return mSuitableEmulators.get(avdNameForBoot)
        }
    }
    final EmulatorVerifier mEmulatorVerifier = new EmulatorVerifier(mDummyLogger, mDummyAndroidInfo.androidHome) {
        List<String> verifyEmulators(final List<String> serials) {
            return serials
        }
    }
    final EmulatorStatfaceReporter mEmulatorStatfaceReporter =
            new EmulatorStatfaceReporter(mDummyLogger, mDummyAndroidInfo, mDummyBuildInfo, mDummyStatfaceInfo)
    final EmulatorController mEmulatorController =
            new EmulatorController(mDummyLogger, mEmulatorFinder, mEmulatorBooter, mEmulatorVerifier,
                    mEmulatorStatfaceReporter)

    @Test
    void testBootEmulators() {
        final int count = 4
        final TestDevicesInfo info = mEmulatorController.bootEmulators(count, false)

        assertThat(info.running).isSubsetOf(mRunningSuitableEmulators.values().toList())
        assertThat(info.booted).isSubsetOf(mSuitableEmulators.values().toList())
        assertThat(info.running + info.booted).hasSize(count)
    }
}
