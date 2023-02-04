package ru.yandex.partner.core.entity.queue;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.partner.core.queue.TaskData;
import ru.yandex.partner.core.queue.TaskFactory;
import ru.yandex.partner.libs.i18n.GettextMsg;

public class TestNonConcurrentTaskFactory implements TaskFactory<TestNonConcurrentTask> {
    @Override
    public Class<TestNonConcurrentTask> getTaskClass() {
        return TestNonConcurrentTask.class;
    }

    @Override
    public int getTypeId() {
        return 254;
    }

    @Override
    public TestNonConcurrentTask fromTaskData(TaskData taskData) {
        try {
            TestNonConcurrentTask.Payload payload = new ObjectMapper().readValue(taskData.getParams(),
                    TestNonConcurrentTask.Payload.class);
            return new TestNonConcurrentTask(payload, taskData);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse TestNonConcurrentTask.Payload", exception);
        }
    }

    @Override
    public int getMaxTries() {
        return 3;
    }

    @Override
    public Duration getTryAfter() {
        return Duration.ofSeconds(0);
    }

    @Override
    public boolean isAllowConcurrentExecution() {
        return false;
    }

    @Override
    public GettextMsg getTitle() {
        return TestTaskTypeMsg.NON_CONCURRENT_TASK;
    }
}
