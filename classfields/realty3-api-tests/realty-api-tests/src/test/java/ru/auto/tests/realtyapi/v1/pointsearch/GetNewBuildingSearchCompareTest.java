package ru.auto.tests.realtyapi.v1.pointsearch;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.BOTTOM_LATITUDE;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.LEFT_LONGITUDE;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.RIGHT_LONGITUDE;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.TOP_LATITUDE;

@Title("GET /newbuildingPointSearch")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetNewBuildingSearchCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(ARTEAMO)
    public void shouldPointSearchStatHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.pointSearch().newbuildingPointSearchRoute()
                .reqSpec(authSpec())
                .bottomLatitudeQuery(BOTTOM_LATITUDE)
                .topLatitudeQuery(TOP_LATITUDE)
                .leftLongitudeQuery(LEFT_LONGITUDE)
                .rightLongitudeQuery(RIGHT_LONGITUDE)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi))
                .whenIgnoringPaths("response.searchQuery.logQueryId"));
    }
}
