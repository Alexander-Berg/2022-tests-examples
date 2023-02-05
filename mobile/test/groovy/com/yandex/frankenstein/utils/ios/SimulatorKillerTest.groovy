package com.yandex.frankenstein.utils.ios

import com.yandex.frankenstein.properties.info.IOSInfo
import com.yandex.frankenstein.utils.CommandLineExecutor
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class SimulatorKillerTest {

    final List<String> mKilledUdids = []

    final IOSInfo mIosInfo = new IOSInfo("", "", "", "", "", "", "")
    final Logger mDummyLogger = [info: {}] as Logger
    CommandLineExecutor mExecutor = new CommandLineExecutor(mDummyLogger) {
        @Override
        CommandLineExecutor execute(final List<String> commandLine) {
            if (commandLine[2] == 'shutdown') {
                addToKilled(commandLine[3])
            }
            return this
        }
    }
    final SimulatorKiller mSimulatorKiller = new SimulatorKiller(mDummyLogger, mIosInfo, mExecutor)

    @Test
    void testMultipleKill() {
        final List<String> udidsForKill = ['udid1', 'udid2']

        mSimulatorKiller.kill(udidsForKill)

        assertThat(mKilledUdids).containsExactlyElementsOf(udidsForKill)
    }
    @Test
    void testOneKill() {
        final String udidForKill = 'udid1'

        mSimulatorKiller.kill(udidForKill)

        assertThat(mKilledUdids).containsExactly(udidForKill)
    }

    private void addToKilled(final String udid) {
        mKilledUdids << udid
    }
}
