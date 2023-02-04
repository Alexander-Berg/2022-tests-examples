package ru.yandex.payments.testing.micronaut_cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {
    private Hooks() {
    }

    @Before
    public static void beforeScenario(Scenario scenario) {
        MicronautCucumberTest.beforeScenario(scenario);
    }

    @After
    public static void afterScenario(Scenario scenario) {
        MicronautCucumberTest.afterScenario(scenario);
    }
}
