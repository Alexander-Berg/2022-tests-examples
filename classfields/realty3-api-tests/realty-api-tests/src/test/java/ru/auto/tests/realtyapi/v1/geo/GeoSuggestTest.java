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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.enums.YesOrNo.YES;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;

/**
 * Created by vicdev on 31.10.17.
 */

@Title("GET /mobileAutocomplete.json (/geosuggest.json)")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GeoSuggestTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Текстовый запрос")
    @Parameterized.Parameter(0)
    public String text;

    @Parameter("Долгота")
    @Parameterized.Parameter(1)
    public String longitude;

    @Parameter("Широта")
    @Parameterized.Parameter(2)
    public String latitude;

    @Parameter("Количество элементов в выдаче")
    @Parameterized.Parameter(3)
    public String resultSize;

    @Parameter("Тип объявления")
    @Parameterized.Parameter(4)
    public String type;

    @Parameter("Категория объявления")
    @Parameterized.Parameter(5)
    public String category;

    @Parameter("Включить расширенный поиск")
    @Parameterized.Parameter(6)
    public String extendedSearch;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "text={0} longitude={1} latitude={2} resultSize={3} type={4} category={5}" +
            " extendedSearch={6}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"Калу", "", "", "", "", "", ""},
                {"Калу", "30.249950", "", "", "", "", ""},
                {"Калу", "30.249950", "60.006211", "", "", "", ""},
                {"Калу", "30.249950", "60.006211", "10", "", "", ""},
                {"Калу", "30.249950", "60.006211", "10", SELL.name(), "", ""},
                {"Калу", "30.249950", "60.006211", "10", SELL.name(), APARTMENT.name(), ""},
                {"Калу", "30.249950", "60.006211", "10", SELL.name(), APARTMENT.name(), YES.name()},
                //todo: добавить параметры
        });
    }

    @Test
    public void shouldMobileAutocompleteNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.geo().geosuggestRoute()
                .reqSpec(authSpec())
                .textQuery(text)
                .longitudeQuery(longitude)
                .latitudeQuery(latitude)
                .resultSizeQuery(resultSize)
                .typeQuery(type)
                .categoryQuery(category)
                .extendedSearchQuery(extendedSearch)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths("response.logQueryId"));
    }


    @Test
    public void shouldGeoSuggestHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.geo().geosuggestRoute()
                .reqSpec(authSpec())
                .textQuery(text)
                .longitudeQuery(longitude)
                .latitudeQuery(latitude)
                .resultSizeQuery(resultSize)
                .typeQuery(type)
                .categoryQuery(category)
                .extendedSearchQuery(extendedSearch)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths("response.logQueryId"));
    }
}
