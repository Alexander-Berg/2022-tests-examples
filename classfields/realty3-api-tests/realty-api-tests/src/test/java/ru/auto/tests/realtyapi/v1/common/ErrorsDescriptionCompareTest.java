package ru.auto.tests.realtyapi.v1.common;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

/**
 * Created by vicdev on 08.11.17.
 */

@Title("GET /errorsDescription")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ErrorsDescriptionCompareTest {

    private static final int UNSUPPORTED_CODE = 26;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;


    @Parameter("Код ошибки индексатора")
    @Parameterized.Parameter(0)
    public int code;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object> getParameters() {
        return IntStream.range(1, 100).filter(s -> s != UNSUPPORTED_CODE).boxed().collect(Collectors.toList());
    }

    @Test
    public void shouldNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.common().buildErrorsDescriptionRoute()
                .reqSpec(authSpec())
                .codeQuery(code)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
