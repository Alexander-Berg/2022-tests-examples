package ru.auto.tests.realtyapi.v1.suggest;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonNodeAbsent;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("GET /suggest/tags")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetSuggestTagsTest {

    private static final int ZERO_COUNT = 0;
    private static final int VALID_COUNT = 3;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.suggest().suggestTagRoute()
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSeeEmptyListWithZeroCount() {
        JsonObject response = api.suggest().suggestTagRoute().reqSpec(authSpec())
                .countQuery(ZERO_COUNT)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response).describedAs("Ответ не должен иметь список")
                .satisfies(r -> MatcherAssert.assertThat(r, jsonNodeAbsent("items")));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeListWithRequestedSize() {
        JsonObject response = api.suggest().suggestTagRoute().reqSpec(authSpec())
                .countQuery(VALID_COUNT)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("items"))
                .describedAs("Ответ должен иметь список заданного размера")
                .hasSize(VALID_COUNT);
    }
}
