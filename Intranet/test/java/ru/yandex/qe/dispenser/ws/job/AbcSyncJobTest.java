package ru.yandex.qe.dispenser.ws.job;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.quartz.utils.ClassUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import ru.yandex.qe.dispenser.ws.abc.ProjectTreeSyncTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbcSyncJobTest extends AbstractJobTest {

    private Scheduler clusteredScheduler;

    protected AbcSyncJobTest() {
        super(new String[]{"development", "sqldao", "secondary", "abc-sync"});
    }

    @Override
    @BeforeAll
    public void beforeClass() throws SchedulerException {
        super.beforeClass();

        final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        clusteredScheduler = autowireCapableBeanFactory.getBean("clusteredScheduler", Scheduler.class);
    }

    @Test
    public void checkAbcSyncJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {

        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("abcSyncTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofMinutes(10).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final ProjectTreeSyncTask mock = mockBeanPostProcessor.getMockByName("projectTreeSyncTask");

        verify(mock, times(1)).update();
    }
}
