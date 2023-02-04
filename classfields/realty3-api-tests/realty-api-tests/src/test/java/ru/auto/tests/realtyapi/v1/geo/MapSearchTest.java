package ru.auto.tests.realtyapi.v1.geo;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import net.javacrumbs.jsonunit.core.Option;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /mapSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MapSearchTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String type;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameter(2)
    public String bottomLatitude;

    @Parameterized.Parameter(3)
    public String topLatitude;

    @Parameterized.Parameter(4)
    public String leftLongitude;

    @Parameterized.Parameter(5)
    public String rightLongitude;

    @Parameterized.Parameter(6)
    public String width;

    @Parameterized.Parameter(7)
    public String height;

    @Parameterized.Parameter(8)
    public String dpi;

    @Parameterized.Parameter(9)
    public String cellSizeInches;

    @Parameterized.Parameter(10)
    public String newFlat;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "type={0} category={1} bottomLatitude={2} topLatitude={3} leftLongitude={4} rightLongitude={5} width={6} height={7} dpi={8} cellSizeInches={9} newFlat={10}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"SELL", "APARTMENT", "53.26863998924679", "57.54565201896538", "35.3632001224147",
                        "39.99986685786601", "1080", "1753", "425", "", ""},
                {"RENT", "APARTMENT", "53.26863998924679", "57.54565201896538", "35.3632001224147",
                        "39.99986685786601", "1080", "1753", "425", "", ""},
                {"SELL", "ROOMS", "53.26863998924679", "57.54565201896538", "35.3632001224147",
                        "39.99986685786601", "1080", "1753", "425", "", ""},
                {"SELL", "HOUSE", "53.26863998924679", "57.54565201896538", "35.3632001224147",
                        "39.99986685786601", "1080", "1753", "425", "", ""},
                {"SELL", "LOT", "53.26863998924679", "57.54565201896538", "35.3632001224147",
                        "39.99986685786601", "1080", "1753", "425", "", ""},
                {"SELL", "COMMERCIAL", "53.26863998924679", "57.54565201896538", "35.3632001224147",
                        "39.99986685786601", "1080", "1753", "425", "", ""},
                {"SELL", "GARAGE", "53.26863998924679", "57.54565201896538", "35.3632001224147",
                        "39.99986685786601", "1080", "1753", "425", "", ""}
        });
    }

    @Test
    public void shouldNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.geo().mapSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(type)
                .categoryQuery(category)
                .viewportBottomLatitudeQuery(bottomLatitude)
                .viewportTopLatitudeQuery(topLatitude)
                .viewportLeftLongitudeQuery(leftLongitude)
                .viewportRightLongitudeQuery(rightLongitude)
                .viewportWidthQuery(width)
                .viewportHeightQuery(height)
                .viewportDPIQuery(dpi)
                .cellSizeInchesQuery(cellSizeInches)
                .newFlatQuery(newFlat)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi))
                .whenIgnoringPaths("response.searchQuery.logQueryId")
                .when(Option.IGNORING_ARRAY_ORDER));
    }
}
