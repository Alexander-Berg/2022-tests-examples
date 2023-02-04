package ru.auto.tests.commons.awaitility;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import lombok.extern.log4j.Log4j;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by vicdev on 21.07.17.
 */
@Log4j
public class AllureConditionEvaluationLogger implements ConditionEvaluationListener<Object> {

    private final TimeUnit timeUnit;

    /**
     * Uses {@link java.util.concurrent.TimeUnit#MILLISECONDS} as unit for elapsed and remaining time.
     */
    public AllureConditionEvaluationLogger() {
        this(MILLISECONDS);
    }

    /**
     * Specifies the {@link java.util.concurrent.TimeUnit} to use as unit for elapsed and remaining time.
     *
     * @param timeUnit The time unit to use.
     */
    public AllureConditionEvaluationLogger(TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        this.timeUnit = timeUnit;
    }

    public void conditionEvaluated(EvaluatedCondition<Object> condition) {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        String description = condition.getDescription();
        long elapsedTime = timeUnit.convert(condition.getElapsedTimeInMS(), MILLISECONDS);
        long remainingTime = timeUnit.convert(condition.getRemainingTimeInMS(), MILLISECONDS);
        String timeUnitAsString = timeUnit.toString().toLowerCase();
        if (condition.isSatisfied()) {
            String message = String.format("%s after %d %s (remaining time %d %s, last poll interval was %d %s)%n", description, elapsedTime, timeUnitAsString, remainingTime, timeUnitAsString,
                    condition.getPollInterval().getValue(), condition.getPollInterval().getTimeUnitAsString());
            log.info(message);
            lifecycle.startStep(
                    UUID.randomUUID().toString(),
                    new StepResult().withName(message).withStatus(Status.PASSED)
            );
            lifecycle.stopStep();
        } else {
            String message = String.format("%s (elapsed time %d %s, remaining time %d %s (last poll interval was %d %s))%n", description, elapsedTime,
                    timeUnitAsString, remainingTime, timeUnitAsString, condition.getPollInterval().getValue(),
                    condition.getPollInterval().getTimeUnitAsString());
            log.info(message);
            lifecycle.startStep(
                    UUID.randomUUID().toString(),
                    new StepResult().withName(message).withStatus(Status.PASSED)
            );
            lifecycle.stopStep();
        }
    }
}