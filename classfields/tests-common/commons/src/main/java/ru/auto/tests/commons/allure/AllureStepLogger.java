package ru.auto.tests.commons.allure;

import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;
import lombok.extern.log4j.Log4j;
import org.aeonbits.owner.ConfigFactory;

import java.util.Deque;
import java.util.LinkedList;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@Log4j
public class AllureStepLogger implements StepLifecycleListener {

    private static final AllureProperties allure = ConfigFactory
            .create(AllureProperties.class, System.getProperties(), System.getenv());
    private Deque<String> names = new LinkedList<>();

    public void beforeStepStart(StepResult result) {
        if (allure.allureStepLoggerEnabled()) {
            names.push(result.getName());
            log.info(getOffset() + " [ -> ] " + defaultIfBlank(result.getDescription(), result.getName()));
        }
    }

    public void afterStepStop(StepResult result) {
        if (allure.allureStepLoggerEnabled()) {
            log.info(getOffset() + " [ <- ] Step Finished!");
            names.poll();
        }
    }

    private String getOffset() {
        return new String(new char[names.isEmpty() ? 0 : names.size() - 1])
                .replaceAll("\0", "   ");
    }


}
