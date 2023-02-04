package ru.auto.tests.realtyapi.v1.geo;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.parseParams;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /mapSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MapSearchOffersTest {

    private static final String BOTTOM_LATITUDE = "viewportBottomLatitude";
    private static final String TOP_LATITUDE = "viewportTopLatitude";
    private static final String LEFT_LONGITUDE = "viewportLeftLongitude";
    private static final String RIGHT_LONGITUDE = "viewportRightLongitude";
    private static final double SCROLL_VALUE = 0.0000001;

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
        return parseParams("offers", "search/map_search_test_cases.txt");
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
                .getAsJsonArray("offers");

        MatcherAssert.assertThat(offers.apply(query), jsonEquals(offers.apply(scroll(query))));
    }

    public static Map<String, String> scroll(Map<String, String> query) {
        Map<String, String> scrollQuery = new HashMap<>(query);
        scrollQuery.put(TOP_LATITUDE, String.valueOf(Double.parseDouble(query.get(TOP_LATITUDE)) + SCROLL_VALUE));
        scrollQuery.put(BOTTOM_LATITUDE, String.valueOf(Double.parseDouble(query.get(BOTTOM_LATITUDE)) - SCROLL_VALUE));
        scrollQuery.put(LEFT_LONGITUDE, String.valueOf(Double.parseDouble(query.get(LEFT_LONGITUDE)) + SCROLL_VALUE));
        scrollQuery.put(RIGHT_LONGITUDE, String.valueOf(Double.parseDouble(query.get(RIGHT_LONGITUDE)) - SCROLL_VALUE));
        return scrollQuery;
    }
}
