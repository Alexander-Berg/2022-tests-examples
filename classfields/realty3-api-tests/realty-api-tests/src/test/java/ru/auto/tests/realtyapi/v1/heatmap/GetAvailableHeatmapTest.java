package ru.auto.tests.realtyapi.v1.heatmap;

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
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /heatmaps/available")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetAvailableHeatmapTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String leftLongitude;

    @Parameterized.Parameter(1)
    public String rightLongitude;

    @Parameterized.Parameter(2)
    public String topLatitude;

    @Parameterized.Parameter(3)
    public String bottomLatitude;

    @Parameterized.Parameter(4)
    public String zoom;

    @Parameterized.Parameters(name = "leftLongitude={0} rightLongitude={1} topLatitude={2} bottomLatitude={3} zoom={4}")
    public static Object[][] getParameters() {
        return new String[][]{
                {"35.3632001224147", "39.99986685786601", "57.54565201896538", "53.26863998924679", "20"}
        };
    }

    @Test
    public void shouldNoDiffWithProductionMapPolygon() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.heatmap().getHeatmapsAvailableRoute().reqSpec(authSpec())
                .leftLongitudeQuery(leftLongitude)
                .rightLongitudeQuery(rightLongitude)
                .topLatitudeQuery(topLatitude)
                .bottomLatitudeQuery(bottomLatitude)
                .zQuery(zoom)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
