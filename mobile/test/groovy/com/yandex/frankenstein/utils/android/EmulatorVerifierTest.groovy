package com.yandex.frankenstein.utils.android

import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class EmulatorVerifierTest {

    private final String mAndroidHome = "/Users/user/Library/Android/sdk"
    private final Logger mDummyLogger = [
            warn: {}
    ] as Logger
    private final File mTestApp = File.createTempFile("prefix", "suffix.apk")
    private final String mTestAppId = "test.app.id"
    private final List<String> mAdbCommandsList = []

    private final AdbExecutor mAdbExecutor = new AdbExecutor(mDummyLogger, mAndroidHome) {
        String getAdbCommandOutput(final List<String> commands) {
            mAdbCommandsList.add(commands.join(" "))
            return ""
        }

        String getAdbShellCommandOutput(final List<String> commands) {
            mAdbCommandsList.add(commands.join(" "))
            return "Success"
        }
    }
    private final EmulatorVerifier mVerifier =
            new EmulatorVerifier(mDummyLogger, mAndroidHome, mTestApp, mTestAppId)


    @Test
    void testVerifyEmulatorInstall() {
        final boolean success = mVerifier.verifyEmulatorInstall(mAdbExecutor)
        assertThat(mAdbCommandsList).containsExactly(
                "push ${mTestApp.absolutePath} /data/local/tmp/test_app.apk".toString(),
                "pm install -r -t /data/local/tmp/test_app.apk",
                "pm uninstall ${mTestAppId}".toString(),
                "rm /data/local/tmp/test_app.apk")
        assertThat(success).isTrue()
    }

    @Test
    void testVerifyEmulatorInstallIfCannotInstall() {
        final AdbExecutor adbExecutor = new AdbExecutor(mDummyLogger, mAndroidHome) {
            String getAdbCommandOutput(final List<String> commands) {
                mAdbCommandsList.add(commands.join(" "))
                return ""
            }

            String getAdbShellCommandOutput(final List<String> commands) {
                mAdbCommandsList.add(commands.join(" "))
                if (commands.contains("install")) {
                    return "Fail"
                }
                return "Success"
            }
        }
        final boolean success = mVerifier.verifyEmulatorInstall(adbExecutor)
        assertThat(mAdbCommandsList).containsExactly(
                "push ${mTestApp.absolutePath} /data/local/tmp/test_app.apk".toString(),
                "pm install -r -t /data/local/tmp/test_app.apk",
                "rm /data/local/tmp/test_app.apk")
        assertThat(success).isFalse()
    }

    @Test
    void testVerifyEmulatorInstallIfCannotUninstall() {
        final AdbExecutor adbExecutor = new AdbExecutor(mDummyLogger, mAndroidHome) {
            String getAdbCommandOutput(final List<String> commands) {
                mAdbCommandsList.add(commands.join(" "))
                return ""
            }

            String getAdbShellCommandOutput(final List<String> commands) {
                mAdbCommandsList.add(commands.join(" "))
                if (commands.contains("uninstall")) {
                    return "Fail"
                }
                return "Success"
            }
        }
        final boolean success = mVerifier.verifyEmulatorInstall(adbExecutor)
        assertThat(mAdbCommandsList).containsExactly(
                "push ${mTestApp.absolutePath} /data/local/tmp/test_app.apk".toString(),
                "pm install -r -t /data/local/tmp/test_app.apk",
                "pm uninstall ${mTestAppId}".toString(),
                "rm /data/local/tmp/test_app.apk")
        assertThat(success).isFalse()
    }

    @Test
    void testVerifyEmulators() {
        final List<String> badEmulatorsSerials = ["serial1", "serial2"]
        final List<String> goodEmulatorsSerials = ["goodSerial1", "goodSerial2"]
        final List<String> emulatorsSerials = goodEmulatorsSerials + badEmulatorsSerials

        final EmulatorVerifier verifier =
                new EmulatorVerifier(mDummyLogger, mAndroidHome, mTestApp, mTestAppId) {
                    boolean verifyEmulatorInstall(final AdbExecutor adbExecutor) {
                        return goodEmulatorsSerials.contains(adbExecutor.serial)
                    }
                }
        final List<String> verifiedSerials = verifier.verifyEmulators(emulatorsSerials)
        assertThat(verifiedSerials).containsExactlyInAnyOrderElementsOf(goodEmulatorsSerials)
    }
}
