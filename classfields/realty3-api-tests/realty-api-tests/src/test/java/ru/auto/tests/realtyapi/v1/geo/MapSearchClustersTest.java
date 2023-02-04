package ru.auto.tests.realtyapi.v1.geo;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonNodePresent;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.parseParams;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.geo.MapSearchOffersTest.scroll;


@Title("GET /mapSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MapSearchClustersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter
    @Parameterized.Parameter
    public Map<String, String> query;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() throws IOException {
        return parseParams("clusters", "search/map_search_test_cases.txt");
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldClustersIsNotEmptyTest() {
        // Эту ручку мы уже не используем, но из-за старых клиентов удалять ни ее, ни тест нельзя. Пока.
        // Подробнее в https://st.yandex-team.ru/REALTYBACK-4054#5f96e761fea2534dc251d0ae
        JsonObject response = api.geo().mapSearchRoute()
                .reqSpec(authSpec())
                .reqSpec(req -> req.addQueryParams(query))
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response).describedAs("Массив clusters не должен быть пустым")
                .satisfies(r -> MatcherAssert.assertThat(r, jsonNodePresent("clusters")));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldHasSameResponseWithScrollTest() {
        Function<Map<String, String>, JsonArray> offers = map -> api.geo().mapSearchRoute()
                .reqSpec(authSpec())
                .reqSpec(req -> req.addQueryParams(map))
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonArray("clusters");

        MatcherAssert.assertThat(offers.apply(query), jsonEquals(offers.apply(scroll(query))));
    }
}
