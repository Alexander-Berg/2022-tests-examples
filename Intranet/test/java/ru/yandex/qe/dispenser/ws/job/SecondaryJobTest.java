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

import ru.yandex.qe.dispenser.domain.entity.EntityLifetimeManager;
import ru.yandex.qe.dispenser.domain.history.QuotaChangeRequestHistoryClearTask;
import ru.yandex.qe.dispenser.domain.history.QuotaHistoryClearTask;
import ru.yandex.qe.dispenser.domain.lots.LotsManager;
import ru.yandex.qe.dispenser.domain.notifications.NotificationManager;
import ru.yandex.qe.dispenser.domain.request.RequestManager;
import ru.yandex.qe.dispenser.ws.ServicesOverCommitValueMetrics;
import ru.yandex.qe.dispenser.ws.abc.UpdateProjectMembers;
import ru.yandex.qe.dispenser.ws.quota.QuotaMaxAggregationJob;
import ru.yandex.qe.dispenser.ws.quota.request.ReCreateRequestTicketsTask;
import ru.yandex.qe.dispenser.ws.staff.StaffSyncTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SecondaryJobTest extends AbstractJobTest {

    private Scheduler clusteredScheduler;

    protected SecondaryJobTest() {
        super(new String[]{"development", "sqldao", "secondary"});
    }

    @Override
    @BeforeAll
    public void beforeClass() throws SchedulerException {
        super.beforeClass();

        final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        clusteredScheduler = autowireCapableBeanFactory.getBean("clusteredScheduler", Scheduler.class);
    }

    @Test
    public void checkQuotaMaxAggregationJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {

        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("quotaMaxAggregationTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofMinutes(60).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final QuotaMaxAggregationJob mock = mockBeanPostProcessor.getMockByName("quotaMaxAggregationJob");

        verify(mock, times(1)).aggregateMaxes();
    }

    @Test
    public void checkServicesOverCommitValueMetrics() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {

        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("serviceOverCommitMetricsTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofMinutes(1).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final ServicesOverCommitValueMetrics mock = mockBeanPostProcessor.getMockByName("servicesOverCommitValueMetrics");

        verify(mock, times(1)).countOverCommit();
    }

    @Test
    public void checkUpdateProjectMembersJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {

        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("updateProjectMembersTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofMinutes(10).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final UpdateProjectMembers mock = mockBeanPostProcessor.getMockByName("updateProjectMembers");

        verify(mock, times(1)).update();
    }

    @Test
    public void checkRemoveOldEntityJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("removeOldEntityTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(100, trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final EntityLifetimeManager mock = mockBeanPostProcessor.getMockByName("entityLifetimeManager");

        verify(mock, times(1)).removeOld();
    }

    @Test
    public void checkSendNotificationJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("sendNotificationsTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofMinutes(1).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final NotificationManager mock = mockBeanPostProcessor.getMockByName("notifier");

        verify(mock, times(1)).sendNotifications();
    }

    @Test
    public void checkLotsUpdateJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("updateLastOverQuotingTsTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofSeconds(3).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final LotsManager mock = mockBeanPostProcessor.getMockByName("lotsManager");

        verify(mock, times(1)).update();
    }

    @Test
    public void checkRemoveOldIdempotentRequestJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("removeOldIdempotentRequestTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofMinutes(60).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final RequestManager mock = mockBeanPostProcessor.getMockByName("requestManager");

        verify(mock, times(1)).removeOld();
    }

    @Test
    public void checkStaffSyncJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("staffSyncTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofMinutes(60).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final StaffSyncTask mock = mockBeanPostProcessor.getMockByName("staffSyncTask");

        verify(mock, times(1)).update();
    }

    @Test
    public void recreateRequestTicketsJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("recreateRequestTicketsTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(2400000, trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final ReCreateRequestTicketsTask mock = mockBeanPostProcessor.getMockByName("reCreateRequestTicketsTask");

        verify(mock, times(1)).createTickets();
    }


    @Test
    public void checkQuotaHistoryClearJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("quotaHistoryClearTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofDays(1).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final QuotaHistoryClearTask mock = mockBeanPostProcessor.getMockByName("quotaHistoryClearTask");

        verify(mock, times(1)).clear();
    }

    @Test
    public void checkQuotaChangeRequestHistoryClearJob() throws SchedulerException, InterruptedException, TimeoutException, ExecutionException {
        final SimpleTrigger trigger = (SimpleTrigger) clusteredScheduler.getTrigger(TriggerKey.triggerKey("quotaChangeRequestHistoryClearTrigger", TriggerKey.DEFAULT_GROUP));

        assertEquals(Duration.ofDays(1).toMillis(), trigger.getRepeatInterval());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, trigger.getMisfireInstruction());
        assertTrue(ClassUtils.isAnnotationPresent(clusteredScheduler.getJobDetail(trigger.getJobKey()).getJobClass(), DisallowConcurrentExecution.class));

        triggerAndWait(clusteredScheduler, trigger);

        final QuotaChangeRequestHistoryClearTask mock = mockBeanPostProcessor.getMockByName("quotaChangeRequestHistoryClearTask");

        verify(mock, times(1)).clear();
    }
}
