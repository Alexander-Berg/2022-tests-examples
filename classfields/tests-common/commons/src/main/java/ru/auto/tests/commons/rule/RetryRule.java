package ru.auto.tests.commons.rule;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.junit4.AllureJunit4;
import io.qameta.allure.util.ResultsUtils;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Objects;

import static io.qameta.allure.model.Status.PASSED;

/**
 * eroshenkoam
 * 10.07.17
 */
public class RetryRule implements TestRule {

    private int attempts;

    private final AllureLifecycle lifecycle;

    public RetryRule() {
        this(Allure.getLifecycle());
    }

    public RetryRule(AllureLifecycle lifecycle) {
        RetryConfig config = ConfigFactory.create(RetryConfig.class, System.getProperties(), System.getenv());
        this.lifecycle = lifecycle;
        this.attempts = config.getAttempts();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (attempts < 0) {
            throw new IllegalArgumentException(String.format("\"attempts\" with value %s not greater than 0", attempts));
        }

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable lastException;
                int current = 0;
                do {
                    try {
                        if (current > 0) {
                            new AllureJunit4().testStarted(description);
                        }
                        base.evaluate();
                        return;
                    } catch (Throwable e) {
                        getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
                            getLifecycle().updateTestCase(uuid, testResult -> testResult
                                    .withStatus(ResultsUtils.getStatus(e).orElse(null))
                                    .withStatusDetails(ResultsUtils.getStatusDetails(e).orElse(null))
                            );

                        });
                        lastException = e;
                    } finally {
                        getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
                            getLifecycle().updateTestCase(uuid, testResult -> {
                                if (Objects.isNull(testResult.getStatus())) {
                                    testResult.setStatus(PASSED);
                                }
                            });
                            getLifecycle().stopTestCase(uuid);
                            getLifecycle().writeTestCase(uuid);
                        });
                    }
                } while (current++ < attempts);
                throw lastException;
            }
        };
    }

    private AllureLifecycle getLifecycle() {
        return lifecycle;
    }
}
