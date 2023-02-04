package ru.yandex.webmaster3.worker.task;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.logbroker.reader.MessageContainer;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.json.JsonMapping;
import ru.yandex.webmaster3.core.worker.client.LogbrokerMultiTopicClient;
import ru.yandex.webmaster3.core.worker.task.TaskResult;
import ru.yandex.webmaster3.core.worker.task.WorkerTaskData;
import ru.yandex.webmaster3.core.worker.task.WorkerTaskPriority;
import ru.yandex.webmaster3.core.worker.task.WorkerTaskType;
import ru.yandex.webmaster3.core.worker.task.model.WorkerTaskDataBatch;
import ru.yandex.webmaster3.core.worker.task.model.WorkerTaskDataWrapper;
import ru.yandex.webmaster3.storage.task.TaskBatchLogYDao;
import ru.yandex.webmaster3.storage.task.TaskStateLogYDao;
import ru.yandex.webmaster3.worker.Task;
import ru.yandex.webmaster3.worker.TaskRegistry;
import ru.yandex.webmaster3.worker.queue.TaskQueueMetrics;
import ru.yandex.webmaster3.worker.queue.TaskScheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * ishalaru
 * 01.04.2020
 **/
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class InfinityWorkerRunnerTest {

    InfinityWorkerRunner infinityWorkerRunner;
    BlockingQueue<MessageContainer> bq = new ArrayBlockingQueue<>(10);
    @Mock
    Task task;
    @Mock
    LogbrokerMultiTopicClient logbrokerMultiTopicClient;
    @Mock
    TaskBatchLogYDao taskBatchLogYDao;
    @Mock
    TaskStateLogYDao taskStateLogYDao;
    @Mock
    TaskScheduler taskScheduler;
    @Mock
    TaskQueueMetrics taskQueueMetrics;
    @Mock
    TaskRegistry taskRegistry;

    @Before
    public void init() throws Exception {
        infinityWorkerRunner = new InfinityWorkerRunner(bq, taskQueueMetrics, taskRegistry, logbrokerMultiTopicClient, taskBatchLogYDao, taskStateLogYDao, taskScheduler);
        Map<WorkerTaskType, Task> map = new HashMap<>();
        when(task.getDataClass()).thenReturn(TestTaskData.class);
        when(task.run(any())).thenReturn(new Task.Result(TaskResult.SUCCESS));
        map.put(WorkerTaskType.TEST_TASK, task);
        when(taskRegistry.getTaskRegistryMap()).thenReturn(map);
        when(taskScheduler.isPaused(any())).thenReturn(false);
    }


    @Test(timeout = 10000)
    @SneakyThrows
    public void positiveTest() {
        final Thread thread = new Thread(infinityWorkerRunner);
        thread.start();
        StreamListener.ReadResponder readResponder = mock(StreamListener.ReadResponder.class);
        TestTaskData testTaskData = new TestTaskData(UUID.randomUUID(), IdUtils.urlToHostId("http://yandex.ru"), 1);
        WorkerTaskDataWrapper workerTaskDataWrapper = new WorkerTaskDataWrapper(WorkerTaskType.TEST_TASK, "123456", JsonMapping.writeValueAsString(testTaskData), WorkerTaskPriority.NORMAL, DateTime.now());
        UUID batchId = UUID.randomUUID();
        when(taskBatchLogYDao.createOrStartBatch(batchId, 1)).thenReturn(TaskBatchLogYDao.Status.NEW);
        WorkerTaskDataBatch workerTaskDataBatch = new WorkerTaskDataBatch(List.of(workerTaskDataWrapper), batchId);
        MessageContainer messageContainer = new MessageContainer(readResponder, List.of(JsonMapping.writeValueAsString(workerTaskDataBatch).getBytes()));
        bq.add(messageContainer);
        while (!bq.isEmpty()) {
            ;
        }
        thread.interrupt();
        thread.join(1000);
        verify(taskBatchLogYDao, times(1)).createOrStartBatch(batchId, 1);
        verify(task, times(1)).run(any());
        verify(taskBatchLogYDao, times(1)).update(batchId, TaskBatchLogYDao.Status.COMPLETED);
    }

    @Test(timeout = 10000)
    @SneakyThrows
    public void positiveWithOldFormatTest() {
        final Thread thread = new Thread(infinityWorkerRunner);
        thread.start();
        StreamListener.ReadResponder readResponder = mock(StreamListener.ReadResponder.class);
        TestTaskData testTaskData = new TestTaskData(UUID.randomUUID(), IdUtils.urlToHostId("http://yandex.ru"), 1);
        WorkerTaskDataWrapper workerTaskDataWrapper = new WorkerTaskDataWrapper(WorkerTaskType.TEST_TASK, "123456", JsonMapping.writeValueAsString(testTaskData), WorkerTaskPriority.NORMAL, DateTime.now());
        UUID batchId = UUID.randomUUID();
        when(taskBatchLogYDao.createOrStartBatch(any(), anyInt())).thenReturn(TaskBatchLogYDao.Status.NEW);
        WorkerTaskDataBatch workerTaskDataBatch = new WorkerTaskDataBatch(List.of(workerTaskDataWrapper), batchId);
        MessageContainer messageContainer = new MessageContainer(readResponder, List.of(JsonMapping.writeValueAsString(workerTaskDataWrapper).getBytes()));
        bq.add(messageContainer);
        while (!bq.isEmpty()) {
            ;
        }
        thread.interrupt();
        thread.join(1000);
        ArgumentCaptor<UUID> ac = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<Integer> acI = ArgumentCaptor.forClass(Integer.class);
        verify(taskBatchLogYDao, times(1)).createOrStartBatch(ac.capture(), acI.capture());
        verify(task, times(1)).run(any());
        verify(taskBatchLogYDao, times(1)).update(ac.getValue(), TaskBatchLogYDao.Status.COMPLETED);
    }

    @Test(timeout = 10000)
    @SneakyThrows
    public void taskCompletedBefore() {

        final Thread thread = new Thread(infinityWorkerRunner);
        thread.start();
        StreamListener.ReadResponder readResponder = mock(StreamListener.ReadResponder.class);
        TestTaskData testTaskData = new TestTaskData(UUID.randomUUID(), IdUtils.urlToHostId("http://yandex.ru"), 1);
        WorkerTaskDataWrapper workerTaskDataWrapper = new WorkerTaskDataWrapper(WorkerTaskType.TEST_TASK, "123456", JsonMapping.writeValueAsString(testTaskData), WorkerTaskPriority.NORMAL, DateTime.now());
        UUID batchId = UUID.randomUUID();
        when(taskBatchLogYDao.createOrStartBatch(batchId, 1)).thenReturn(TaskBatchLogYDao.Status.COMPLETED);
        WorkerTaskDataBatch workerTaskDataBatch = new WorkerTaskDataBatch(List.of(workerTaskDataWrapper), batchId);
        MessageContainer messageContainer = new MessageContainer(readResponder, List.of(JsonMapping.writeValueAsString(workerTaskDataBatch).getBytes()));
        bq.add(messageContainer);
        while (!bq.isEmpty()) {
            ;
        }
        thread.interrupt();
        thread.join(1000);
        verify(taskBatchLogYDao, times(1)).createOrStartBatch(batchId, 1);
        verify(task, never()).run(any());
        verify(taskBatchLogYDao, never()).update(batchId, TaskBatchLogYDao.Status.COMPLETED);
    }


    @Test(timeout = 10000)
    @SneakyThrows
    public void taskRollMessage() {
        final Thread thread = new Thread(infinityWorkerRunner);
        thread.start();
        StreamListener.ReadResponder readResponder = mock(StreamListener.ReadResponder.class);
        TestTaskData testTaskData = new TestTaskData(UUID.randomUUID(), IdUtils.urlToHostId("http://yandex.ru"), 1);
        WorkerTaskDataWrapper workerTaskDataWrapper = new WorkerTaskDataWrapper(WorkerTaskType.TEST_TASK, "123456", JsonMapping.writeValueAsString(testTaskData), WorkerTaskPriority.NORMAL, DateTime.now());
        UUID batchId = UUID.randomUUID();
        when(taskBatchLogYDao.createOrStartBatch(Mockito.eq(batchId), Mockito.eq(1))).thenReturn(TaskBatchLogYDao.Status.IN_PROGRESS_ON_CONTROL);
        WorkerTaskDataBatch workerTaskDataBatch = new WorkerTaskDataBatch(List.of(workerTaskDataWrapper), batchId);
        MessageContainer messageContainer = new MessageContainer(readResponder, List.of(JsonMapping.writeValueAsString(workerTaskDataBatch).getBytes()));
        bq.add(messageContainer);
        while (!bq.isEmpty()) {
            ;
        }
        thread.interrupt();
        thread.join(1000);
        verify(taskBatchLogYDao, times(1)).createOrStartBatch(batchId, 1);
        verify(task, never()).run(any());
        verify(taskBatchLogYDao, never()).update(batchId, TaskBatchLogYDao.Status.COMPLETED);
        verify(logbrokerMultiTopicClient, times(1)).write(anyList(), any());
        verify(readResponder, times(1)).commit();
    }

    @Test(timeout = 10000)
    @SneakyThrows
    public void taskRollbackByMessage() {
        when(taskScheduler.isPaused(any())).thenReturn(true);
        final Thread thread = new Thread(infinityWorkerRunner);
        thread.start();
        StreamListener.ReadResponder readResponder = mock(StreamListener.ReadResponder.class);
        TestTaskData testTaskData = new TestTaskData(UUID.randomUUID(), IdUtils.urlToHostId("http://yandex.ru"), 1);
        WorkerTaskDataWrapper workerTaskDataWrapper = new WorkerTaskDataWrapper(WorkerTaskType.TEST_TASK, "123456", JsonMapping.writeValueAsString(testTaskData), WorkerTaskPriority.NORMAL, DateTime.now());
        UUID batchId = UUID.randomUUID();
        when(taskBatchLogYDao.createOrStartBatch(batchId, 1)).thenReturn(TaskBatchLogYDao.Status.NEW);
        WorkerTaskDataBatch workerTaskDataBatch = new WorkerTaskDataBatch(List.of(workerTaskDataWrapper), batchId);
        MessageContainer messageContainer = new MessageContainer(readResponder, List.of(JsonMapping.writeValueAsString(workerTaskDataBatch).getBytes()));
        bq.add(messageContainer);
        while (!bq.isEmpty()) {
            ;
        }
        thread.interrupt();
        thread.join(1000);
        verify(taskBatchLogYDao, times(1)).createOrStartBatch(batchId, 1);
        verify(task, never()).run(any());
        verify(taskBatchLogYDao, times(1)).update(batchId, TaskBatchLogYDao.Status.COMPLETED);
    }

    @Test(timeout = 10000)
    @SneakyThrows
    public void taskRollMessageAndThenProcessing() {
        final Thread thread = new Thread(infinityWorkerRunner);
        thread.start();
        StreamListener.ReadResponder readResponder = mock(StreamListener.ReadResponder.class);
        TestTaskData testTaskData = new TestTaskData(UUID.randomUUID(), IdUtils.urlToHostId("http://yandex.ru"), 1);
        WorkerTaskDataWrapper workerTaskDataWrapper = new WorkerTaskDataWrapper(WorkerTaskType.TEST_TASK, "123456", JsonMapping.writeValueAsString(testTaskData), WorkerTaskPriority.NORMAL, DateTime.now());
        UUID batchId = UUID.randomUUID();
        when(taskBatchLogYDao.createOrStartBatch(batchId, 1)).thenReturn(TaskBatchLogYDao.Status.IN_PROGRESS_ON_CONTROL).thenReturn(TaskBatchLogYDao.Status.IN_PROGRESS);
        final DateTime dateTime = DateTime.now().minusHours(2);
        WorkerTaskDataBatch workerTaskDataBatch = new WorkerTaskDataBatch(List.of(workerTaskDataWrapper), batchId);
        MessageContainer messageContainer = new MessageContainer(readResponder, List.of(JsonMapping.writeValueAsString(workerTaskDataBatch).getBytes()));
        bq.add(messageContainer);
        WorkerTaskDataBatch workerTaskDataBatch_1 = new WorkerTaskDataBatch(List.of(workerTaskDataWrapper), batchId);
        StreamListener.ReadResponder readResponder_1 = mock(StreamListener.ReadResponder.class);
        MessageContainer messageContainer_1 = new MessageContainer(readResponder_1, List.of(JsonMapping.writeValueAsString(workerTaskDataBatch).getBytes()));
        bq.add(messageContainer_1);
        while (!bq.isEmpty()) {
            ;
        }
        thread.interrupt();
        thread.join(1000);
        verify(taskBatchLogYDao, times(2)).createOrStartBatch(batchId, 1);
        verify(task, times(1)).run(any());
        verify(taskBatchLogYDao, times(1)).update(batchId, TaskBatchLogYDao.Status.COMPLETED);
        verify(logbrokerMultiTopicClient, times(1)).write(anyList(), any());
        verify(readResponder, times(1)).commit();
        verify(readResponder_1, times(1)).commit();

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
