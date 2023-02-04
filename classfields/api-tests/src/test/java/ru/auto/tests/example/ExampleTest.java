package ru.auto.tests.example;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.example.module.ExampleApiModule;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.example.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.example.ResponseSpecBuilders.validatedWith;


@DisplayName("GET /..")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ExampleApiModule.class)
public class ExampleTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    public void simpleTest() {
        Map<String, Integer> inventory = api.store().getInventory().executeAs(validatedWith(shouldBeCode(SC_OK)));
        Assertions.assertThat(inventory.keySet().size()).isGreaterThan(0);
    }
}
