package ru.auto.tests.example;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.example.module.ExampleApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Arrays;
import java.util.List;

import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.example.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.example.ResponseSpecBuilders.validatedWith;

@DisplayName("GET /...")
@GuiceModules(ExampleApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExampleParametrizedTest {

    private static final String X_REQUEST_ID_HEADER = "X-Request-Id";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter(X_REQUEST_ID_HEADER)
    @Parameterized.Parameter(0)
    public String headerValue;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<String> getParameters() {
        return Arrays.asList("123", "456");
    }

    @Test
    public void simpleTest() {
        api.store().reqSpec(r -> r.addParam(X_REQUEST_ID_HEADER, headerValue)).getInventory().executeAs(validatedWith(shouldBeCode(SC_OK)));
    }
}
