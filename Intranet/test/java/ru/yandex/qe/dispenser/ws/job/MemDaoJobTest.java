package ru.yandex.qe.dispenser.ws.job;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.quartz.utils.ClassUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import ru.yandex.qe.dispenser.domain.distributed.DistributedManager;
import ru.yandex.qe.dispenser.domain.entity.EntityLifetimeManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MemDaoJobTest extends AbstractJobTest {

    private Scheduler nonClusteredScheduler;

    protected MemDaoJobTest() {
        super(new String[]{"development", "memdao"});
    }

    @Override
    @BeforeAll
    public void beforeClass() throws SchedulerException {
        super.beforeClass();

        final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        nonClusteredScheduler = autowireCapableBeanFactory.getBean("nonClusteredScheduler", Scheduler.class);
    }

    @Test
    public void distributedManagerUpdateJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) nonClusteredScheduler.getTrigger(TriggerKey.triggerKey("distributedManagerUpdateTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(1000, trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(nonClusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(nonClusteredScheduler, trigger);

        final DistributedManager mock = mockBeanPostProcessor.getMockByName("distributedManager");

        verify(mock, atLeastOnce()).update();
    }

    @Test
    public void distributedManagerDumpJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) nonClusteredScheduler.getTrigger(TriggerKey.triggerKey("distributedManagerDumpTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(5000, trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(nonClusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(nonClusteredScheduler, trigger);

        final DistributedManager mock = mockBeanPostProcessor.getMockByName("distributedManager");

        verify(mock, times(1)).dump();
    }

    @Test
    public void distributedManagerSyncJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) nonClusteredScheduler.getTrigger(TriggerKey.triggerKey("distributedManagerSyncTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(500, trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(nonClusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(nonClusteredScheduler, trigger);

        final DistributedManager mock = mockBeanPostProcessor.getMockByName("distributedManager");

        verify(mock, atLeastOnce()).sync();
    }

    @Disabled
    @Test
    public void checkRemoveOldEntityMemDaoJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) nonClusteredScheduler.getTrigger(TriggerKey.triggerKey("removeOldEntityMemDaoTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(100, trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(nonClusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(nonClusteredScheduler, trigger);

        final EntityLifetimeManager mock = mockBeanPostProcessor.getMockByName("entityLifetimeManager");

        verify(mock, atLeastOnce()).removeOld();
    }

}
