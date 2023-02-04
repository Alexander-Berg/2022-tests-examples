package ru.yandex.webmaster3.storage.util.fs.persistence;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * @author avhaliullin
 */
public class WriteAheadLogTest {
    ch.qos.logback.classic.Logger logger;
    Level initLevel;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void muteLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger = context.getLogger(WriteAheadLog.class);
        initLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    @After
    public void unmuteLogging() {
        logger.setLevel(initLevel);
    }

    @Test
    public void testReplay() throws Exception {
        try (WriteAheadLog wal = new WriteAheadLog(tmpFolder.newFolder("test-replay"), 9, false)) {

            wal.replay((reqId, b, offset, len) -> Assert.fail("Shouldn't replay anything from empty log"), 100);

            List<byte[]> messages = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append(i % 10);
                messages.add(sb.toString().getBytes());
            }

            for (byte[] msg : messages) {
                wal.log(msg);
                wal.maintenance();
            }

            Accumulator acc = new Accumulator();
            wal.replay(acc, 100);
            assertMessageListEquals(messages, acc.acc);
        }
    }

    @Test
    public void testReplayLimit() throws Exception {
        try (WriteAheadLog wal = new WriteAheadLog(tmpFolder.newFolder("test-replay-limit"), 9, false)) {

            wal.replay((reqId, b, offset, len) -> Assert.fail("Shouldn't replay anything from empty log"), 100);

            List<byte[]> messages = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append(i % 10);
                messages.add(sb.toString().getBytes());
            }

            int halfWay = messages.size() / 2;
            long lastReqId = 0L;
            for (int i = 0; i < halfWay; i++) {
                lastReqId = wal.log(messages.get(i));
                wal.maintenance();
            }

            for (int i = 0; i < halfWay + 1; i++) {
                Accumulator acc = new Accumulator();
                wal.replay(acc, i);
                assertMessageListEquals(messages.subList(0, i), acc.acc);
            }
            {
                Accumulator acc = new Accumulator();
                wal.replay(acc, 100000);
                assertMessageListEquals(messages.subList(0, halfWay), acc.acc);
            }

            wal.releaseIncluding(lastReqId);

            for (int i = halfWay; i < messages.size(); i++) {
                wal.log(messages.get(i));
                wal.maintenance();
            }

            for (int i = halfWay; i < messages.size() + 1; i++) {
                Accumulator acc = new Accumulator();
                wal.replay(acc, i - halfWay);
                assertMessageListEquals(messages.subList(halfWay, i), acc.acc);
            }
            {
                Accumulator acc = new Accumulator();
                wal.replay(acc, 100000);
                assertMessageListEquals(messages.subList(halfWay, messages.size()), acc.acc);
            }

        }
    }

    @Test
    public void testReplayWithCheckpoint() throws Exception {
        try (WriteAheadLog wal = new WriteAheadLog(tmpFolder.newFolder("test-replay-checkpoint"), 9, false)) {

            List<byte[]> messages = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append(i % 10);
                messages.add(sb.toString().getBytes());
            }

            int halfWay = messages.size() / 2;
            long reqId = 0L;
            for (int i = 0; i < halfWay; i++) {
                reqId = wal.log(messages.get(i));
                wal.maintenance();
            }

            {
                Accumulator acc = new Accumulator();
                wal.replay(acc, 100);
                assertMessageListEquals(messages.subList(0, halfWay), acc.acc);
            }
            wal.releaseIncluding(reqId);
            wal.maintenance();
            wal.replay((rId, b, offset, len) -> Assert.fail("Shouldn't replay anything from empty log"), 100);

            for (int i = halfWay; i < messages.size(); i++) {
                wal.log(messages.get(i));
                wal.maintenance();
            }
            {
                Accumulator acc = new Accumulator();
                wal.replay(acc, 100);
                assertMessageListEquals(messages.subList(halfWay, messages.size()), acc.acc);
            }
        }
    }

    @Test
    public void testRecovery() throws Exception {
        File workDir = tmpFolder.newFolder("test-recovery");
        List<byte[]> messages = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append(i % 10);
            messages.add(sb.toString().getBytes());
        }

        int halfWay = messages.size() / 2;
        try (WriteAheadLog wal = new WriteAheadLog(workDir, 9, false)) {

            for (int i = 0; i < messages.size(); i++) {
                long reqId = wal.log(messages.get(i));
                if (i == halfWay) {
                    wal.releaseExcluding(reqId);
                }
            }
        }

        try (WriteAheadLog wal = new WriteAheadLog(workDir, 9, false)) {
            {
                Accumulator acc = new Accumulator();
                wal.replay(acc, 100);
                assertMessageListEquals(messages.subList(halfWay, messages.size()), acc.acc);
            }
            wal.maintenance();

            byte[] lastMsg = new byte[]{0, 1, 2, 3, 4};
            long reqId = wal.log(lastMsg);
            wal.releaseExcluding(reqId);
            {
                Accumulator acc = new Accumulator();
                wal.replay(acc, 100);
                assertMessageListEquals(Lists.newArrayList(lastMsg), acc.acc);
            }
        }
    }

    @Test
    public void testDiskSpace() throws Exception {
        int indexRecordSize = 100; //Ленивая оценка сверху
        int messageSize = 10;
        int dumpedMessageSize = messageSize + 4;
        int maxPartSize = 9;
        int windowSize = 20;

        int maxParts = 2 * (windowSize + maxPartSize) / maxPartSize;
        int maxDiskUsage = WriteAheadLog.MAX_CONTROL_SIZE_BYTES + maxParts * (indexRecordSize + maxPartSize * dumpedMessageSize);

        File workDir = tmpFolder.newFolder("test-disk");

        byte[] msg = new byte[messageSize];

        try (WriteAheadLog wal = new WriteAheadLog(workDir, maxPartSize, false)) {
            Queue<Long> ids = new ArrayDeque<>();
            for (int i = 0; i < 1000; i++) {
                ids.add(wal.log(msg));
                if (ids.size() >= windowSize) {
                    long reqId = ids.poll();
                    wal.releaseExcluding(reqId);
                }
                wal.maintenance();
                long totalSize = 0;
                for (File f : workDir.listFiles()) {
                    totalSize += f.length();
                }
                Assert.assertTrue("Disk usage should be below " + maxDiskUsage + ", actual: " + totalSize, totalSize <= maxDiskUsage);
            }
        }
    }

    private static void assertMessageListEquals(List<byte[]> expected, List<byte[]> actual) {
        Assert.assertEquals("Size should be equal", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertArrayEquals("Difference at offset " + i, expected.get(i), actual.get(i));
        }
    }

    private static class Accumulator implements WALRecordConsumer {
        final List<byte[]> acc = new ArrayList<>();

        @Override
        public void consume(long reqId, byte[] b, int offset, int len) {
            acc.add(Arrays.copyOfRange(b, offset, offset + len));
        }
    }
}
