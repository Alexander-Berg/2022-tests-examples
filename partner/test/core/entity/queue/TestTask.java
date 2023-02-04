package ru.yandex.partner.core.entity.queue;

import java.time.Duration;

import ru.yandex.partner.core.queue.AbstractTask;
import ru.yandex.partner.core.queue.TaskData;
import ru.yandex.partner.core.queue.TaskPayload;

public class TestTask extends AbstractTask<TestTask.Payload, Void> {

    public TestTask(Payload payload, TaskData savedTaskData) {
        super(payload, savedTaskData);
    }

    @Override
    public Void execute() {
        System.out.println("executing concurrent task");
        return null;
    }

    @Override
    public TaskData getTaskData() {
        return getSavedTaskData();
    }

    @Override
    public Duration getEstimatedTime() {
        return Duration.ofSeconds(3);
    }

    public static class Payload implements TaskPayload {
        public static Payload of() {
            return new Payload();
        }

        @Override
        public String serializeParams() {
            return null;
        }

        @Override
        public int getTypeId() {
            return TestTaskType.TEST_TASK.getTypeId();
        }

        @Override
        public Long getGroupId() {
            return null;
        }
    }
}
