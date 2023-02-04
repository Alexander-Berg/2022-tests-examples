package ru.yandex.qe.dispenser.quartz.listener;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

public class MonitoringSchedulerListener implements SchedulerListener {
    @Override
    public void jobScheduled(final Trigger trigger) {

    }

    @Override
    public void jobUnscheduled(final TriggerKey triggerKey) {

    }

    @Override
    public void triggerFinalized(final Trigger trigger) {

    }

    @Override
    public void triggerPaused(final TriggerKey triggerKey) {

    }

    @Override
    public void triggersPaused(final String triggerGroup) {

    }

    @Override
    public void triggerResumed(final TriggerKey triggerKey) {

    }

    @Override
    public void triggersResumed(final String triggerGroup) {

    }

    @Override
    public void jobAdded(final JobDetail jobDetail) {

    }

    @Override
    public void jobDeleted(final JobKey jobKey) {

    }

    @Override
    public void jobPaused(final JobKey jobKey) {

    }

    @Override
    public void jobsPaused(final String jobGroup) {

    }

    @Override
    public void jobResumed(final JobKey jobKey) {

    }

    @Override
    public void jobsResumed(final String jobGroup) {

    }

    @Override
    public void schedulerError(final String msg, final SchedulerException cause) {

    }

    @Override
    public void schedulerInStandbyMode() {

    }

    @Override
    public void schedulerStarted() {

    }

    @Override
    public void schedulerStarting() {

    }

    @Override
    public void schedulerShutdown() {

    }

    @Override
    public void schedulerShuttingdown() {

    }

    @Override
    public void schedulingDataCleared() {

    }
}
