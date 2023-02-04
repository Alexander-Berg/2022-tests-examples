package ru.yandex.webmaster3.worker.queue;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.solomon.metric.SolomonMetricRegistry;
import ru.yandex.webmaster3.core.solomon.metric.SolomonTimerConfiguration;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.worker.task.WorkerTaskData;
import ru.yandex.webmaster3.core.worker.task.WorkerTaskPriority;
import ru.yandex.webmaster3.core.worker.task.WorkerTaskType;
import ru.yandex.webmaster3.worker.Task;
import ru.yandex.webmaster3.worker.TaskRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aherman
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class TaskQueueTest {
    private static final Logger log = LoggerFactory.getLogger(TaskQueueTest.class);

    public static final long THREAD_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(60);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testQueue() throws IOException, InterruptedException {
        System.out.println(tmpFolder.getRoot());
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queue");

        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);

        addTasks(taskQueue, hostId, 0, 50);
        pollTaskNoWait(taskQueue, hostId, 0, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @Test
    public void testSaveLoadFull() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queueFull");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        addTasks(taskQueue, hostId, 0, 50);
        taskQueue.shutdown();

        taskQueue = createQueue(tasks, queueFolder);
        pollTaskNoWait(taskQueue, hostId, 0, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @Test
    public void testSaveLoadPartial() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queuePartial");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        addTasks(taskQueue, hostId, 0, 50);
        pollTaskNoWait(taskQueue, hostId, 0, 5);
        taskQueue.shutdown();

        taskQueue = createQueue(tasks, queueFolder);
        pollTaskNoWait(taskQueue, hostId, 5, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @Test
    public void testSaveLoadPartial1() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queuePartial1");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        addTasks(taskQueue, hostId, 0, 50);
        pollTaskNoWait(taskQueue, hostId, 0, 15);
        taskQueue.shutdown();

        taskQueue = createQueue(tasks, queueFolder);
        pollTaskNoWait(taskQueue, hostId, 15, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @Test
    public void testSaveLoadPartial2() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queuePartial2");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        addTasks(taskQueue, hostId, 0, 50);
        pollTaskNoWait(taskQueue, hostId, 0, 25);
        taskQueue.shutdown();

        taskQueue = createQueue(tasks, queueFolder);
        pollTaskNoWait(taskQueue, hostId, 25, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @Test
    public void testSaveLoadPartial3() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queuePartial3");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        addTasks(taskQueue, hostId, 0, 50);
        pollTaskNoWait(taskQueue, hostId, 0, 45);
        taskQueue.shutdown();

        taskQueue = createQueue(tasks, queueFolder);
        pollTaskNoWait(taskQueue, hostId, 45, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @Test
    public void testThreads() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queueThreads");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        AtomicBoolean readFull = new AtomicBoolean(false);

        CyclicBarrier barrier = new CyclicBarrier(2);
        Thread readThread = new Thread(() -> {
            try {
                barrier.await();
                pollTaskWait(taskQueue, hostId, 0, 50);
                readFull.set(true);
            } catch (InterruptedException | BrokenBarrierException e) {
            }
        }, "testThreads-readThread");
        readThread.setDaemon(true);
        readThread.start();

        Thread writeThread = new Thread(() -> {
            try {
                barrier.await();
                addTasks(taskQueue, hostId, 0, 50);
            } catch (IOException | InterruptedException | BrokenBarrierException e) {
                Assert.fail(e.getMessage());
            }
        }, "testThreads-writeThread");
        writeThread.setDaemon(true);
        writeThread.start();
        writeThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(writeThread.isAlive());
        readThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(readThread.isAlive());
        Assert.assertTrue(readFull.get());
    }

    @Test
    public void testThreads1() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queueThreads1");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        AtomicBoolean readFull = new AtomicBoolean(false);

        CyclicBarrier barrier = new CyclicBarrier(2);

        Thread readThread = new Thread(() -> {
            try {
                barrier.await();
                pollTaskWait(taskQueue, hostId, 0, 50);
                readFull.set(true);
            } catch (InterruptedException | BrokenBarrierException e) {
            }
        }, "testThreads1-readThread");
        readThread.setDaemon(true);
        readThread.start();

        Thread writeThread = new Thread(() -> {
            try {
                addTasks(taskQueue, hostId, 0, 25);
                barrier.await();
                addTasks(taskQueue, hostId, 25, 50);
            } catch (IOException | InterruptedException | BrokenBarrierException e) {
                Assert.fail(e.getMessage());
            }
        }, "testThreads1-writeThread");
        writeThread.setDaemon(true);
        writeThread.start();
        writeThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(writeThread.isAlive());
        readThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(readThread.isAlive());
        Assert.assertTrue(readFull.get());
    }

    @Test
    public void testThreads2() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queueThreads2");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        AtomicBoolean readFull = new AtomicBoolean(false);

        CyclicBarrier barrier = new CyclicBarrier(2);

        Thread readThread = new Thread(() -> {
            try {
                barrier.await();
                pollTaskWait(taskQueue, hostId, 0, 50);
                readFull.set(true);
            } catch (InterruptedException | BrokenBarrierException e) {
            }
        }, "testThreads2-readThread");
        readThread.setDaemon(true);
        readThread.start();

        Thread writeThread = new Thread(() -> {
            try {
                addTasks(taskQueue, hostId, 0, 45);
                barrier.await();
                addTasks(taskQueue, hostId, 45, 50);
            } catch (IOException | InterruptedException | BrokenBarrierException e) {
                Assert.fail(e.getMessage());
            }
        }, "testThreads2-writeThread");
        writeThread.setDaemon(true);
        writeThread.start();
        writeThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(writeThread.isAlive());
        readThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(readThread.isAlive());
        Assert.assertTrue(readFull.get());
    }

    @Test
    public void testThreads3() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queueThreads2");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);
        AtomicBoolean readFull = new AtomicBoolean(false);

        CyclicBarrier barrier = new CyclicBarrier(2);

        Thread readThread = new Thread(() -> {
            try {
                barrier.await();
                pollTaskWait(taskQueue, hostId, 0, 50);
                readFull.set(true);
            } catch (InterruptedException | BrokenBarrierException e) {
            }
        }, "testThreads3-readThread");
        readThread.setDaemon(true);
        readThread.start();

        Thread writeThread = new Thread(() -> {
            try {
                addTasks(taskQueue, hostId, 0, 50);
                barrier.await();
            } catch (IOException | InterruptedException | BrokenBarrierException e) {
                Assert.fail(e.getMessage());
            }
        }, "testThreads3-writeThread");
        writeThread.setDaemon(true);
        writeThread.start();
        writeThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(writeThread.isAlive());
        readThread.join(THREAD_WAIT_MILLIS);
        Assert.assertFalse(readThread.isAlive());
        Assert.assertTrue(readFull.get());
    }

    @Test
    public void testClearQueue1() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("clearQueue1");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);

        taskQueue.clear();

        addTasks(taskQueue, hostId, 0, 50);
        pollTaskNoWait(taskQueue, hostId, 0, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @Test
    public void testClearQueue2() throws Exception {
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("clearQueue2");
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);

        addTasks(taskQueue, hostId, 0, 25);
        taskQueue.clear();
        addTasks(taskQueue, hostId, 25, 50);
        pollTaskNoWait(taskQueue, hostId, 25, 50);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    /**
     * Тестируем приоритеты
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testPriorities1() throws IOException, InterruptedException {
        System.out.println(tmpFolder.getRoot());
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");
        Map<WorkerTaskType, Task> tasks = Collections.singletonMap(WorkerTaskType.TEST_TASK, new TestTask());
        File queueFolder = tmpFolder.newFolder("queue");

        TestTaskQueue taskQueue = createQueue(tasks, queueFolder);

        addTasks(taskQueue, hostId, 0, 25, WorkerTaskPriority.NORMAL);
        addTasks(taskQueue, hostId, 0, 5, WorkerTaskPriority.HIGH);
        addTasks(taskQueue, hostId, 25, 50, WorkerTaskPriority.NORMAL);
        // сперва должны идти приоритетные задачи
        pollTaskNoWait(taskQueue, hostId, 0, 5, WorkerTaskPriority.HIGH);
        pollTaskNoWait(taskQueue, hostId, 0, 50, WorkerTaskPriority.NORMAL);

        Assert.assertNull(taskQueue.pollNoWait());
    }

    @NotNull
    private TestTaskQueue createQueue(Map<WorkerTaskType, Task> tasks, File queueFolder) throws IOException {
        SolomonMetricRegistry solomonMetricRegistry = new SolomonMetricRegistry();
        SolomonTimerConfiguration solomonTimerConfiguration= new SolomonTimerConfiguration();
        solomonTimerConfiguration.setEnable(false);

        TaskHostStatistics taskHostStatistics = new TaskHostStatistics() {
            @Override
            public void recordHostTask(WebmasterHostId hostId, WorkerTaskType taskType) {
            }
        };

        TaskRegistry taskRegistry = Mockito.mock(TaskRegistry.class);
        Mockito.when(taskRegistry.getTaskRegistryMap()).thenReturn(tasks);

        TaskQueueMetrics taskQueueMetrics = new TaskQueueMetrics(taskHostStatistics, taskRegistry, solomonMetricRegistry);
        taskQueueMetrics.setSolomonTimerConfiguration(solomonTimerConfiguration);
        taskQueueMetrics.init();

        TaskScheduler taskScheduler = new TaskScheduler(taskRegistry, taskQueueMetrics);
        taskScheduler.init();

        TestTaskQueue taskQueue = new TestTaskQueue(taskRegistry, queueFolder, taskScheduler, taskQueueMetrics);
        taskQueue.setMemoryQueueSize(2);
        taskQueue.setTaskQueueChunkSize(10);
        taskQueue.init();
        return taskQueue;
    }

    private void addTasks(TestTaskQueue taskQueue, WebmasterHostId hostId, int from, int to,
                          WorkerTaskPriority priority) throws IOException {
        for (int i = from; i < to; i++) {
            UUID taskUUID = UUID.nameUUIDFromBytes(new byte[]{(byte) i});

            TaskId taskId = new TaskId(WorkerTaskType.TEST_TASK, hostId, taskUUID);
            TestTaskData data = new TestTaskData(taskUUID, hostId, i);
            TaskRunData taskRunData = new TaskRunData(taskId, TaskRunType.PRIMARY, data, priority);

            taskQueue.enqueueTask(taskRunData);
            log.debug("{} added", i);
        }
    }

    private void addTasks(TestTaskQueue taskQueue, WebmasterHostId hostId, int from, int to) throws IOException {
        addTasks(taskQueue, hostId, from, to, WorkerTaskPriority.NORMAL);
    }

    private void pollTaskNoWait(TestTaskQueue taskQueue, WebmasterHostId hostId, int from, int to,
                                WorkerTaskPriority priority) throws InterruptedException {
        for (int i = from; i < to; i++) {
            UUID expectedTaskUUID = UUID.nameUUIDFromBytes(new byte[]{(byte) i});

            TaskRunData taskRunData = taskQueue.pollNoWait();
            log.debug("{} read", ((TestTaskData) taskRunData.getTaskData()).getI());
            Assert.assertEquals(i, ((TestTaskData) taskRunData.getTaskData()).getI());

            TaskId taskId = taskRunData.getTaskId();
            Assert.assertEquals(WorkerTaskType.TEST_TASK, taskId.getTaskType());
            Assert.assertEquals(expectedTaskUUID, taskId.getTaskUUID());
            Assert.assertEquals(hostId, taskId.getHostId());
            Assert.assertEquals(priority, taskRunData.getTaskPriority());
        }
    }

    private void pollTaskNoWait(TestTaskQueue taskQueue, WebmasterHostId hostId, int from, int to) throws InterruptedException {
        pollTaskNoWait(taskQueue, hostId, from, to, WorkerTaskPriority.NORMAL);
    }

    private void pollTaskWait(TestTaskQueue taskQueue, WebmasterHostId hostId, int from, int to) throws InterruptedException {
        for (int i = from; i < to; i++) {
            UUID expectedTaskUUID = UUID.nameUUIDFromBytes(new byte[]{(byte) i});

            TaskRunData taskRunData = taskQueue.pollTask();
            log.debug("{} read", ((TestTaskData) taskRunData.getTaskData()).getI());
            Assert.assertEquals(i, ((TestTaskData) taskRunData.getTaskData()).getI());

            TaskId taskId = taskRunData.getTaskId();
            Assert.assertEquals(WorkerTaskType.TEST_TASK, taskId.getTaskType());
            Assert.assertEquals(expectedTaskUUID, taskId.getTaskUUID());
            Assert.assertEquals(hostId, taskId.getHostId());
        }
    }

    private static class TestTaskQueue extends TaskQueue {
        public TestTaskQueue(TaskRegistry taskRegistry, File taskQueueFolder, TaskScheduler taskScheduler, TaskQueueMetrics taskQueueMetrics) {
            super(taskRegistry, taskQueueFolder, taskScheduler, taskQueueMetrics);
        }
    }

    private static class TestTask extends Task<TestTaskData> {
        @Override
        public Result run(TestTaskData data) throws Exception {
            return null;
        }

        @Override
        public Class<TestTaskData> getDataClass() {
            return TestTaskData.class;
        }
    }

    private static class TestTaskData extends WorkerTaskData {
        private final int i;

        public TestTaskData(@JsonProperty("taskId") UUID taskId,
                            @JsonProperty("hostId") WebmasterHostId hostId,
                            @JsonProperty("i") int i) {
            super(taskId, hostId);
            this.i = i;
        }

        public int getI() {
            return i;
        }

        @Override
        public WorkerTaskType getTaskType() {
            return WorkerTaskType.TEST_TASK;
        }

        @Override
        public String getShortDescription() {
            return null;
        }
    }

}
