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


@Title("GET /heatmap/{heatmap}/tile")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetTileHeatmapTest {

    private static final int X = 55;
    private static final int Y = 37;
    private static final int ZOOM = 12;

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
        Function<ApiClient, byte[]> request = apiClient -> apiClient.heatmap().getHeatmapTileRoute()
                .reqSpec(authSpec())
                .heatmapPath(heatmap)
                .xQuery(X)
                .yQuery(Y)
                .zQuery(ZOOM)
                .execute(validatedWith(shouldBeCode(SC_OK))).asByteArray();

        assertThat(request.apply(api), is(request.apply(prodApi)));
    }
}
