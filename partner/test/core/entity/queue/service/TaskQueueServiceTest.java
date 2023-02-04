package ru.yandex.partner.core.entity.queue.service;

import java.util.Optional;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.queue.TestNonConcurrentTask;
import ru.yandex.partner.core.entity.queue.TestTask;
import ru.yandex.partner.core.entity.queue.TestTaskType;
import ru.yandex.partner.core.entity.queue.repository.TaskRepository;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.queue.TaskMultistate;
import ru.yandex.partner.core.queue.Task;
import ru.yandex.partner.core.queue.TaskData;
import ru.yandex.partner.core.queue.TaskExecutionResult;
import ru.yandex.partner.dbschema.partner.tables.records.QueueRecord;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.partner.core.CoreConstants.SYSTEM_CRON_USER_ID;
import static ru.yandex.partner.dbschema.partner.Tables.QUEUE;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class TaskQueueServiceTest {

    @Autowired
    TaskRepository taskRepository;
    @Autowired
    private TaskQueueService taskQueueService;

    @Autowired
    private DSLContext dslContext;
    @Autowired
    private TaskQueueExecutor taskQueueExecutor;

    @Test
    void testTask() {
        taskQueueService.enqueue(TestTask.Payload.of());
        Optional<Task<?, ?>> optionalTask = taskQueueService.grabTask(TestTask.class);
        assertTrue(optionalTask.isPresent());
        Task<?, ?> task = optionalTask.get();
        Object result = task.execute();
        taskQueueService.finishTask(task, TaskExecutionResult.success(result));
    }


    @Test
    void testNonConcurrentTask() {
        taskQueueService.enqueue(TestNonConcurrentTask.Payload.of(123L));
        Optional<Task<?, ?>> optionalTask = taskQueueService.grabTask(TestNonConcurrentTask.class);
        assertTrue(optionalTask.isPresent());
        Task<?, ?> task = optionalTask.get();
        Object result = task.execute();
        taskQueueService.finishTask(task, TaskExecutionResult.success(result));
    }

    @Test
    void testMaxTries() {
        taskQueueService.enqueue(TestNonConcurrentTask.Payload.of(2L));

        int tries = 0;
        while (taskQueueExecutor.doOneTask(TestNonConcurrentTask.class)) {
            tries += 1;
            restartTheOnlyTask();
        }

        assertEquals(3, tries);
        QueueRecord queueRecord1 = dslContext.selectFrom(QUEUE).fetchOne();
        assertEquals(queueRecord1.getLog(), "Always fail for someId == 2");
        assertEquals(queueRecord1.getMultistate(), 8L);
    }

    @Test
    void testOneRetry() {
        taskQueueService.enqueue(TestNonConcurrentTask.Payload.of(1L));

        assertTrue(taskQueueExecutor.doOneTask(TestNonConcurrentTask.class));

        QueueRecord queueRecordFailed = dslContext.selectFrom(QUEUE).fetchOne();

        assertEquals(queueRecordFailed.getLog(), "Failed one time for someId == 1");
        assertEquals(queueRecordFailed.getErrorData(), "\"Error data for someId = 1\"");
        assertEquals(queueRecordFailed.getMultistate(), 16L);

        restartTheOnlyTask();

        assertTrue(taskQueueExecutor.doOneTask(TestNonConcurrentTask.class));
        QueueRecord queueRecordSuccess = dslContext.selectFrom(QUEUE).fetchOne();

        assertEquals(queueRecordSuccess.getMultistate(), 4L);
        assertEquals(queueRecordSuccess.getResult(), "\"Result of non-concurrent task execution\"");
    }

    @Test
    void checkCorrectUpdateTaskWithException() {
        TaskData taskData = TaskData.newBuilder()
                .withMultistate(new TaskMultistate())
                .withTypeId(TestTaskType.TEST_TASK.getTypeId())
                .withParams("params")
                .withUserId(SYSTEM_CRON_USER_ID)
                .withTries(0)
                .build();

        taskData = taskRepository.insertTask(taskData);

        var testTask = new TestTask(TestTask.Payload.of(), taskData);

        //save result to db
        var npe = new NullPointerException();
        taskQueueService.finishTask(testTask, TaskExecutionResult.failure(npe.getMessage()));

        //check task after error - error data must be null
        taskData = taskRepository.get(taskData.getId());

        assertThat(taskData.getErrorData()).isNull();
    }


    // todo: non-concurrent for the same group_id
    // todo: non-concurrent for the different group_ids
    private void restartTheOnlyTask() {
        QueueRecord queueRecord = dslContext.selectFrom(QUEUE).fetchOne();
        if (queueRecord.getMultistate().equals(16L)) {
            queueRecord.setMultistate(0L);
            queueRecord.setGrabbedAt(null);
            queueRecord.setGrabbedBy(null);
            queueRecord.setGrabbedUntil(null);
            dslContext.update(QUEUE).set(queueRecord).execute();
        }
    }
}
