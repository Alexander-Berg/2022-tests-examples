package ru.yandex.partner.core.entity.queue.service;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.queue.TestNonConcurrentTask;
import ru.yandex.partner.core.entity.queue.repository.TaskRepository;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.tables.Queue;
import ru.yandex.partner.dbschema.partner.tables.records.QueueRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.partner.dbschema.partner.Tables.QUEUE;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class TaskQueueExecutorTest {
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    private TaskQueueService taskQueueService;

    @Autowired
    private DSLContext dslContext;
    @Autowired
    private TaskQueueExecutor taskQueueExecutor;

    private long taskPayloadCounter = 123;

    @Test
    void testDoOneTask() {
        enqueueTasks(1);
        assertTrue(taskQueueExecutor.doOneTask(TestNonConcurrentTask.class));
    }

    @Test
    void testDoMultipleTask() {
        enqueueTasks(4);

        taskQueueExecutor.doMultipleTask(TestNonConcurrentTask.class, 3);

        assertEquals(getCompletedTasks().size(), 3);
    }

    @Test
    void testDoMultipleTaskWithEmptyQueue() {
        taskQueueExecutor.doMultipleTask(TestNonConcurrentTask.class, 3);

        assertEquals(getCompletedTasks().size(), 0);
    }

    @Test
    void testDoMultipleTaskMultipleRun() {
        enqueueTasks(5);

        taskQueueExecutor.doMultipleTask(TestNonConcurrentTask.class, 3);
        taskQueueExecutor.doMultipleTask(TestNonConcurrentTask.class, 3);

        assertEquals(getCompletedTasks().size(), 5);
    }

    private void enqueueTasks(int count) {
        for (int i = 0; i < count; i++) {
            taskPayloadCounter++;
            taskQueueService.enqueue(TestNonConcurrentTask.Payload.of(taskPayloadCounter));
        }
    }

    private @NotNull Result<QueueRecord> getCompletedTasks() {
        return dslContext.selectFrom(QUEUE)
                .where(Queue.QUEUE.MULTISTATE.eq(4L))
                .fetch();
    }
}
