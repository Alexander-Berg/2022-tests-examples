package ru.auto.tests.realtyapi.v1.pointsearch;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.BOTTOM_LATITUDE;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.LEFT_LONGITUDE;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.RIGHT_LONGITUDE;
import static ru.auto.tests.realtyapi.v1.testdata.PointSearch.TOP_LATITUDE;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferCategory;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferType;

@Title("GET /point/simplePointSearch")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSimplePointsSearchCompareTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter
    @Parameterized.Parameter(0)
    public String type;

    @Parameter
    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameters(name = "type={0} category={1}")
    public static List<Object[]> getParameters() {
        List<Object[]> parameters = new ArrayList<>();
        defaultOfferCategory().forEach(category ->
                defaultOfferType().forEach(type ->
                        parameters.add(new Object[]{type.getValue(), category.getValue()})));
        return parameters;
    }

    @Test
    public void shouldPointSearchStatHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.pointSearch().simplePointSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(type)
                .categoryQuery(category)
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
