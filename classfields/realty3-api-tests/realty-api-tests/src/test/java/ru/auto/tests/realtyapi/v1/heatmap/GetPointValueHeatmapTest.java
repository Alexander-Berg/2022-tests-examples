package ru.auto.tests.realtyapi.v1.heatmap;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.common.ErrorsDescriptionCompareTest;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /heatmap/{heatmap}/point/value")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetPointValueHeatmapTest {

    private static final double LONGITUDE = 30.404011;
    private static final double LATITUDE = 59.962791;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String heatmap;

    @SuppressWarnings("ConstantConditions")
    @Parameterized.Parameters(name = "heatmap={0}")
    public static List getParameters() throws IOException {
        InputStream dataStream = ErrorsDescriptionCompareTest.class.getClassLoader()
                .getResourceAsStream("heatmap/heatmaps.txt");

        return IOUtils.readLines(dataStream);
    }

    @Test
    public void shouldNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.heatmap().getHeatmapPointValueRoute()
                .reqSpec(authSpec())
                .heatmapPath(heatmap)
                .longitudeQuery(LONGITUDE)
                .latitudeQuery(LATITUDE)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
