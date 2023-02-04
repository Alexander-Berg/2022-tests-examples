package ru.yandex.partner.core.entity.queue.repository;

import java.time.Duration;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.queue.TestNonConcurrentTask;
import ru.yandex.partner.core.entity.queue.TestNonConcurrentTaskFactory;
import ru.yandex.partner.core.entity.queue.TestTask;
import ru.yandex.partner.core.entity.queue.TestTaskFactory;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.queue.TaskMultistate;
import ru.yandex.partner.core.queue.TaskData;
import ru.yandex.partner.dbschema.partner.tables.records.QueueRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.partner.dbschema.partner.Tables.QUEUE;

@CoreTest
@ExtendWith(MySqlRefresher.class)
public class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DSLContext dslContext;

    @Autowired
    private TestTaskFactory testTaskFactory;

    @Autowired
    private TestNonConcurrentTaskFactory testNonConcurrentTaskFactory;

    @Test
    public void testInsertTask() {

        TaskData taskData = generateTask(1, 1L);

        TaskData taskDataInserted = taskRepository.insertTask(taskData);

        System.out.println(taskDataInserted);

        assertEquals(1L, taskDataInserted.getId());

        QueueRecord queueRecord = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted.getId()))
                .fetchOne();

        assertEquals(1L, queueRecord.getId().longValue());
        assertEquals(1L, queueRecord.getGroupId());

    }

    @Test
    public void testUpdateTask() {

        TaskData taskData = generateTask(1, 1L);

        TaskData taskDataInserted = taskRepository.insertTask(taskData);

        taskDataInserted.setGroupId(2L);

        taskRepository.updateTask(taskDataInserted);

        QueueRecord queueRecord = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted.getId()))
                .fetchOne();

        assertEquals(1L, queueRecord.getId().longValue());
        assertEquals(2L, queueRecord.getGroupId());

    }

    @Test
    public void testGrabTaskConcurrent() {

        TaskData taskData1 = generateTask(testTaskFactory.getTypeId(), 1L);
        TaskData taskDataInserted1 = taskRepository.insertTask(taskData1);

        TaskData taskData2 = generateTask(testTaskFactory.getTypeId(), 1L);
        TaskData taskDataInserted2 = taskRepository.insertTask(taskData2);

        TaskData taskDataGrabbed1 = taskRepository.pickFreeTask(TestTask.class).orElse(null);
        taskRepository.lockTask(taskDataGrabbed1, Duration.ofSeconds(60));
        TaskData taskDataGrabbed2 = taskRepository.pickFreeTask(TestTask.class).orElse(null);
        taskRepository.lockTask(taskDataGrabbed2, Duration.ofSeconds(60));

        assertNotNull(taskDataGrabbed1);
        assertNotNull(taskDataGrabbed2);

        assertNotEquals(taskData2.getId(), taskDataGrabbed1.getId());

        assertNotNull(taskDataGrabbed1.getGrabbedBy());
        assertNotNull(taskDataGrabbed1.getGrabbedAt());
        assertNotNull(taskDataGrabbed1.getGrabbedUntil());

        assertNotNull(taskDataGrabbed2.getGrabbedBy());
        assertNotNull(taskDataGrabbed2.getGrabbedAt());
        assertNotNull(taskDataGrabbed2.getGrabbedUntil());

        QueueRecord queueRecord1 = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted1.getId()))
                .fetchOne();

        QueueRecord queueRecord2 = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted2.getId()))
                .fetchOne();

        assertNotNull(queueRecord1);
        assertNotNull(queueRecord1.getGrabbedBy());
        assertNotNull(queueRecord1.getGrabbedAt());
        assertNotNull(queueRecord1.getGrabbedUntil());

        assertNotNull(queueRecord2);
        assertNotNull(queueRecord2.getGrabbedBy());
        assertNotNull(queueRecord2.getGrabbedAt());
        assertNotNull(queueRecord2.getGrabbedUntil());

    }

    @Test
    public void testGrabTaskNonConcurrent() {

        TaskData taskData1 = generateTask(testNonConcurrentTaskFactory.getTypeId(), 1L);
        TaskData taskDataInserted1 = taskRepository.insertTask(taskData1);

        TaskData taskData2 = generateTask(testNonConcurrentTaskFactory.getTypeId(), 1L);
        TaskData taskDataInserted2 = taskRepository.insertTask(taskData2);

        TaskData taskDataGrabbed1 = taskRepository.pickFreeTask(TestNonConcurrentTask.class).orElse(null);
        taskRepository.lockTask(taskDataGrabbed1, Duration.ofSeconds(60));
        TaskData taskDataGrabbed2 = taskRepository.pickFreeTask(TestNonConcurrentTask.class).orElse(null);

        assertNotNull(taskDataGrabbed1);
        assertNull(taskDataGrabbed2);

        assertNotNull(taskDataGrabbed1.getGrabbedBy());
        assertNotNull(taskDataGrabbed1.getGrabbedAt());
        assertNotNull(taskDataGrabbed1.getGrabbedUntil());

        QueueRecord queueRecord1 = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted1.getId()))
                .fetchOne();

        QueueRecord queueRecord2 = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted2.getId()))
                .fetchOne();

        assertNotNull(queueRecord1);
        assertNotNull(queueRecord1.getGrabbedBy());
        assertNotNull(queueRecord1.getGrabbedAt());
        assertNotNull(queueRecord1.getGrabbedUntil());

        assertNotNull(queueRecord2);
        assertNull(queueRecord2.getGrabbedBy());
        assertNull(queueRecord2.getGrabbedAt());
        assertNull(queueRecord2.getGrabbedUntil());

    }

    @Test
    public void testGrabTaskNonConcurrentDistinctGroups() {

        TaskData taskData1 = generateTask(testNonConcurrentTaskFactory.getTypeId(), 1L);
        TaskData taskDataInserted1 = taskRepository.insertTask(taskData1);

        TaskData taskData2 = generateTask(testNonConcurrentTaskFactory.getTypeId(), 2L);
        TaskData taskDataInserted2 = taskRepository.insertTask(taskData2);

        TaskData taskDataGrabbed1 = taskRepository.pickFreeTask(TestNonConcurrentTask.class).orElse(null);
        taskRepository.lockTask(taskDataGrabbed1, Duration.ofSeconds(60));
        TaskData taskDataGrabbed2 = taskRepository.pickFreeTask(TestNonConcurrentTask.class).orElse(null);
        taskRepository.lockTask(taskDataGrabbed2, Duration.ofSeconds(60));

        assertNotNull(taskDataGrabbed1);
        assertNotNull(taskDataGrabbed2);

        assertNotEquals(taskData2.getId(), taskDataGrabbed1.getId());

        assertNotNull(taskDataGrabbed1.getGrabbedBy());
        assertNotNull(taskDataGrabbed1.getGrabbedAt());
        assertNotNull(taskDataGrabbed1.getGrabbedUntil());

        assertNotNull(taskDataGrabbed2.getGrabbedBy());
        assertNotNull(taskDataGrabbed2.getGrabbedAt());
        assertNotNull(taskDataGrabbed2.getGrabbedUntil());

        QueueRecord queueRecord1 = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted1.getId()))
                .fetchOne();

        QueueRecord queueRecord2 = dslContext.selectFrom(QUEUE)
                .where(QUEUE.ID.eq(taskDataInserted2.getId()))
                .fetchOne();

        assertNotNull(queueRecord1);
        assertNotNull(queueRecord1.getGrabbedBy());
        assertNotNull(queueRecord1.getGrabbedAt());
        assertNotNull(queueRecord1.getGrabbedUntil());

        assertNotNull(queueRecord2);
        assertNotNull(queueRecord2.getGrabbedBy());
        assertNotNull(queueRecord2.getGrabbedAt());
        assertNotNull(queueRecord2.getGrabbedUntil());

    }

    private TaskData generateTask(int typeId, long groupId) {
        return TaskData.newBuilder()
                .withId(null)
                .withMultistate(new TaskMultistate())
                .withAddDt(null)
                .withStartDt(null)
                .withEndDt(null)
                .withTypeId(typeId)
                .withGroupId(groupId)
                .withParams(null)
                .withUserId(1009L)
                .withLog(null)
                .withErrorData(null)
                .withResult(null)
                .withTries(0)
                .withGrabbedBy(null)
                .withGrabbedAt(null)
                .withGrabbedUntil(null)
                .build();
    }

}
