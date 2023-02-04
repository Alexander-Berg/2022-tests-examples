package ru.yandex.qe.dispenser.quartz.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class MonitoringJobListener implements JobListener {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void jobToBeExecuted(final JobExecutionContext context) {
    }

    @Override
    public void jobExecutionVetoed(final JobExecutionContext context) {

    }

    @Override
    public void jobWasExecuted(final JobExecutionContext context, final JobExecutionException jobException) {
    }
}
