package ru.auto.tests.realtyapi.v2.flatPlans;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /flatPlans/suggest")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetSuggestTest {

    private static final String VALID_ADDRESS = "Россия, Приморский край, Владивосток, проспект Острякова, 6";
    private static final String VALID_FLAT_PLAN = "II-29";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.flatPlans().search()
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    @Issue("REALTYBACK-2334")
    @Ignore("HTTP/1.1 500 Internal Server Error")
    public void shouldSee400WithNoAddress() {
        api.flatPlans().search().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeEmptyResponseWithInvalidAddress() {
        JsonObject response = api.flatPlans().search().reqSpec(authSpec())
                .addressQuery(getRandomString())
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(response, jsonEquals(new JsonObject()));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldFlatPlanBeValid() {
        JsonObject response = api.flatPlans().search().reqSpec(authSpec())
                .addressQuery(VALID_ADDRESS)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        Assertions.assertThat(response.get("seriesName").getAsString())
                .describedAs("У дома должна быть правильная серия")
                .isEqualTo(VALID_FLAT_PLAN);
    }
}
