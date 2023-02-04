package ru.auto.tests.commons.rule;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.auto.tests.commons.utils.MarkerManager;

import javax.inject.Inject;

/**
 * Created by vicdev on 14.08.17.
 */
public class MarkerResource extends ExternalResource {

    public static final String MS_MARKER = "ms_marker";

    @Inject
    private MarkerManager markerManager;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before(description);
                base.evaluate();
            }
        };
    }

    protected void before(Description description) throws Throwable {
        markerManager.generate(description);
        Allure.getLifecycle().updateTestCase(testResult -> testResult.getParameters().add(new Parameter()
                .withName(MS_MARKER).withValue(markerManager.getId())));
    }
}
