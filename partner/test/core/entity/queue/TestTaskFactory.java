package ru.yandex.partner.core.entity.queue;

import java.time.Duration;

import ru.yandex.partner.core.queue.TaskData;
import ru.yandex.partner.core.queue.TaskFactory;
import ru.yandex.partner.libs.i18n.GettextMsg;

public class TestTaskFactory implements TaskFactory<TestTask> {
    @Override
    public Class<TestTask> getTaskClass() {
        return TestTask.class;
    }

    @Override
    public int getTypeId() {
        return TestTaskType.TEST_TASK.getTypeId();
    }

    @Override
    public TestTask fromTaskData(TaskData taskData) {
        return new TestTask(TestTask.Payload.of(), taskData);
    }

    @Override
    public int getMaxTries() {
        return 3;
    }

    @Override
    public Duration getTryAfter() {
        return Duration.ofSeconds(30);
    }

    @Override
    public boolean isAllowConcurrentExecution() {
        return true;
    }

    @Override
    public GettextMsg getTitle() {
        return TestTaskTypeMsg.REGULAR_TASK;
    }
}
