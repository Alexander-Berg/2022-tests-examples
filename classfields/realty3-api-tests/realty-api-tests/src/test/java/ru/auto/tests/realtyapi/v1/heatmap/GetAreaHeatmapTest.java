package ru.auto.tests.realtyapi.v1.heatmap;

import com.carlosbecker.guice.GuiceModules;
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

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /heatmap/{heatmap}/area")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetAreaHeatmapTest {

    private static final double LONGITUDE = 55.75363;
    private static final double LATITUDE = 37.62007;
    private static final int ZOOM = 12;
    private static final int SIZE = 450;

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
        Function<ApiClient, byte[]> request = apiClient -> apiClient.heatmap().getHeatmapAreaRoute()
                .reqSpec(authSpec())
                .heatmapPath(heatmap)
                .longitudeQuery(LONGITUDE)
                .latitudeQuery(LATITUDE)
                .zQuery(ZOOM)
                .widthQuery(SIZE)
                .heightQuery(SIZE)
                .execute(validatedWith(shouldBeCode(SC_OK))).asByteArray();

        assertThat(request.apply(api), is(request.apply(prodApi)));
    }
}
