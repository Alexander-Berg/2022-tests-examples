package ru.auto.tests.realtyapi.v1.geo;

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

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;



@Title("GET /regionAutodetect.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RegionAutodetectMapPolygonCompareTest {
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
    public String mapPolygon;

    @Parameterized.Parameters(name = "mapPolygon={0}")
    public static Object[] getParameters() {
        return new String[]{
                // Санкт-Петербург
                "59.946741,30.348143;59.952556,30.343341;59.954964,30.331750;59.952006,30.318629;59.946493,30.312578;" +
                        "59.940447,30.316072;59.937411,30.327063;59.939164,30.339113;59.940320,30.341992;" +
                        "59.940926,30.343341;59.946741,30.348143",
        };
    }

    @Test
    public void shouldNoDiffWithProductionMapPolygon() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.geo().regionAutodetectRoute()
                .reqSpec(authSpec())
                .mapPolygonQuery(mapPolygon)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
