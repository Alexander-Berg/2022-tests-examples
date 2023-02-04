package ru.yandex.webmaster3.core.worker.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.yandex.webmaster3.core.data.WebmasterHostId;

import java.util.UUID;

/**
 * @author avhaliullin
 */
public class Test404ErrorsAbsenceTaskData extends WorkerTaskData {
    public Test404ErrorsAbsenceTaskData(WebmasterHostId hostId) {
        super(hostId);
    }

    protected Test404ErrorsAbsenceTaskData(@JsonProperty("taskId") UUID taskId, @JsonProperty("hostId") WebmasterHostId hostId) {
        super(taskId, hostId);
    }

    @Override
    public WorkerTaskType getTaskType() {
        return WorkerTaskType.TEST_404_ERRORS_ABSENCE;
    }

    @Override
    public String getShortDescription() {
        return null;
    }
}
