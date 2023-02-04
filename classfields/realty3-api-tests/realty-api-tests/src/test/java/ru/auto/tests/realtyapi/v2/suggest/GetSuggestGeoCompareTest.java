package ru.auto.tests.realtyapi.v2.suggest;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

@Title("GET /suggest/geo")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSuggestGeoCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String text;

    @Parameterized.Parameter(1)
    public String latitude;

    @Parameterized.Parameter(2)
    public String longitude;

    @Parameterized.Parameter(3)
    public String region;

    @Parameterized.Parameter(4)
    public boolean highlight;


    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "text={0} latitude={1} longitude={2} region={3} highlight={4}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"ленин", "60", "30", "", false}
        };
    }

    @Test
    public void shouldGeoSuggestHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.suggest().geoSuggestRoute()
                .reqSpec(authSpec())
                .textQuery(text)
                .latitudeQuery(latitude)
                .longitudeQuery(longitude)
                .highlightQuery(highlight)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
