package ru.yandex.qe.dispenser.quartz.listener;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;

public class MonitoringTriggerListener implements TriggerListener {
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void triggerFired(final Trigger trigger, final JobExecutionContext context) {

    }

    @Override
    public boolean vetoJobExecution(final Trigger trigger, final JobExecutionContext context) {
        return false;
    }

    @Override
    public void triggerMisfired(final Trigger trigger) {

    }

    @Override
    public void triggerComplete(final Trigger trigger, final JobExecutionContext context,
                                final Trigger.CompletedExecutionInstruction triggerInstructionCode) {

    }
}
