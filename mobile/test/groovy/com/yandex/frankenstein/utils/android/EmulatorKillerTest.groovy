package com.yandex.frankenstein.utils.android


import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class EmulatorKillerTest {

    private final List<String> mGoodSerials = ["goodSerial1", "goodSerial2"]
    private final List<String> mAdbExecutorCommands = []

    private final String mAndroidHome = "/Users/user/Library/Android/sdk"
    private final Logger mDummyLogger = [
            info: {}
    ] as Logger
    private final AdbExecutor mAdbExecutor = new AdbExecutor(mDummyLogger, mAndroidHome) {
        void runAdbCommand(final List<String> commands, final String serial) {
            mAdbExecutorCommands.add("${commands.join(" ")} $serial".toString())
        }
    }
    private final AdbProperties mAdbProperties = new AdbProperties(mDummyLogger, mAdbExecutor) {
        List<String> getSerialNumbers() {
            return []
        }
    }

    private final EmulatorKiller mEmulatorKiller = new EmulatorKiller(mDummyLogger, mAdbExecutor, mAdbProperties)

    @Test
    void testKill() {
        mEmulatorKiller.kill(mGoodSerials)
        assertThat(mAdbExecutorCommands).containsExactly(
                "emu kill goodSerial1",
                "emu kill goodSerial2")
    }
}
