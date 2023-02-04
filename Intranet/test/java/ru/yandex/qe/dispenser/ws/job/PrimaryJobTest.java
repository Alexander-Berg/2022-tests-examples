package ru.yandex.qe.dispenser.ws.job;

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

import ru.yandex.qe.dispenser.domain.dao.person.StaffCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class PrimaryJobTest extends AbstractJobTest {

    private Scheduler nonClusteredScheduler;

    protected PrimaryJobTest() {
        super(new String[]{"development", "sqldao"});
    }

    @Override
    @BeforeAll
    public void beforeClass() throws SchedulerException {
        super.beforeClass();

        final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        nonClusteredScheduler = autowireCapableBeanFactory.getBean("nonClusteredScheduler", Scheduler.class);
    }

    @Test
    public void staffCacheRefreshJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) nonClusteredScheduler.getTrigger(TriggerKey.triggerKey("staffCacheRefreshTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(1800000, trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(nonClusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(nonClusteredScheduler, trigger);

        final StaffCache mock = mockBeanPostProcessor.getMockByName("staffCache");

        verify(mock, atLeastOnce()).scheduledRefresh();
    }

}
