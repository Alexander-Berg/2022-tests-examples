package ru.yandex.webmaster3.worker.task;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.webmaster3.core.worker.task.PeriodicTaskState;
import ru.yandex.webmaster3.core.worker.task.PeriodicTaskType;
import ru.yandex.webmaster3.core.worker.task.TaskResult;
import ru.yandex.webmaster3.worker.PeriodicTask;
import ru.yandex.webmaster3.worker.PeriodicTasksLoggingDisable;
import ru.yandex.webmaster3.worker.TaskSchedule;

/**
 * @author aherman
 */
@PeriodicTasksLoggingDisable
public class TestTask extends PeriodicTask<PeriodicTaskState> {
    private static final Logger log = LoggerFactory.getLogger(TestTask.class);

    @Override
    public PeriodicTaskType getType() {
        return PeriodicTaskType.TEST_TASK;
    }

    @Override
    public Result run(UUID runId) throws Exception {
        log.info("Test periodic task: scheduler is working");
        Thread.sleep(TimeUnit.SECONDS.toMillis(70));
        return new Result(TaskResult.SUCCESS);
    }

    @Override
    public TaskSchedule getSchedule() {
        return TaskSchedule.startByCron("*/20 * * * * *");
    }
}
