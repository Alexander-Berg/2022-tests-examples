package ru.auto.tests.publicapi.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by vicdev on 09.10.17.
 */

@DisplayName("GET /reference/catalog/{category}/suggest")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SuggestCompareTest {

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public CategoryEnum category;

    @Parameter("Марка")
    @Parameterized.Parameter(1)
    public String mark;

    @Parameter("Модель")
    @Parameterized.Parameter(2)
    public String model;

    @Parameter("Год")
    @Parameterized.Parameter(3)
    public String year;

    @Parameter("Тип кузова")
    @Parameterized.Parameter(4)
    public String bodyType;

    @Parameter("Супер поколение")
    @Parameterized.Parameter(5)
    public String superGenId;

    @Parameter("Тип двигателя")
    @Parameterized.Parameter(6)
    public String engineType;

    @Parameter("Тип привода")
    @Parameterized.Parameter(7)
    public String gearType;

    @Parameter("Тип трансмиссии")
    @Parameterized.Parameter(8)
    public String transmission;

    @Parameter("Id ттх")
    @Parameterized.Parameter(9)
    public String techParamId;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}-{1}-{2}-{3}-{4}-{5}-{6}-{7}-{8}-{9}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {CARS, "", "", "", "", "", "", "", "", ""},

                {CARS, "HONDA", "", "", "", "", "", "", "", ""},
                {CARS, "HONDA", "CIVIC", "", "", "", "", "", "", ""},
                {CARS, "HONDA", "CIVIC", "2008", "", "", "", "", "", ""},
                {CARS, "HONDA", "CIVIC", "2008", "HATCHBACK_5_DOORS", "", "", "", "", ""},

                {CARS, "BMW", "", "", "", "", "", "", "", ""},
                {CARS, "BMW", "8ER", "", "", "", "", "", "", ""},
                {CARS, "BMW", "8ER", "2018", "CABRIO", "", "", "", "", ""},
                {CARS, "BMW", "8ER", "2018", "CABRIO", "21315985", "DIESEL", "", "", ""},

                {CARS, "AUDI", "", "", "", "", "", "", "", ""},
                {CARS, "AUDI", "A6", "", "", "", "", "", "", ""},
                {CARS, "AUDI", "A6", "2017", "", "", "", "", "", ""},
                {CARS, "AUDI", "A6", "2017", "SEDAN", "20246005", "", "", "", ""},
                {CARS, "AUDI", "A6", "2017", "SEDAN", "20246005", "GASOLINE", "", "", ""},
                {CARS, "AUDI", "A6", "2017", "SEDAN", "20246005", "GASOLINE", "FORWARD_CONTROL", "", ""},
                {CARS, "AUDI", "A6", "2017", "SEDAN", "20246005", "GASOLINE", "ALL_WHEEL_DRIVE", "", ""},
                {CARS, "AUDI", "A6", "2017", "SEDAN", "20246005", "GASOLINE", "FORWARD_CONTROL", "ROBOT", ""},
                {CARS, "AUDI", "A6", "2017", "WAGON_5_DOORS", "20246005", "", "", "", ""},
                {CARS, "AUDI", "A6", "2017", "WAGON_5_DOORS", "20246005", "DIESEL", "ALL_WHEEL_DRIVE", "", ""},
                {CARS, "AUDI", "A6", "2017", "WAGON_5_DOORS", "20246005", "DIESEL", "ALL_WHEEL_DRIVE", "AUTOMATIC", ""},

                {CARS, "MAZDA", "", "", "", "", "", "", "", ""},
                {CARS, "MAZDA", "3", "", "", "", "", "", "", ""},
                {CARS, "MAZDA", "3", "2019", "", "", "", "", "", ""},
                {CARS, "MAZDA", "3", "2019", "SEDAN", "", "", "", "", ""},
                {CARS, "MAZDA", "3", "2019", "SEDAN", "21514240", "", "", "", ""},
                {CARS, "MAZDA", "3", "2019", "SEDAN", "21514240", "DIESEL", "", "", ""},
                {CARS, "MAZDA", "3", "2019", "SEDAN", "21514240", "DIESEL", "FORWARD_CONTROL", "AUTOMATIC", ""},

                {CARS, "VAZ", "", "", "", "", "", "", "", ""},
                {CARS, "VAZ", "GRANTA", "", "", "", "", "", "", ""},
                {CARS, "VAZ", "GRANTA", "2016", "LIFTBACK", "", "", "", "", ""},
                {CARS, "VAZ", "GRANTA", "2016", "LIFTBACK", "7684102", "GASOLINE", "FORWARD_CONTROL", "ROBOT", ""},

                {CARS, "SKODA", "", "", "", "", "", "", "", ""},
                {CARS, "SKODA", "OCTAVIA", "", "", "", "", "", "", ""},
                {CARS, "SKODA", "OCTAVIA", "2017", "", "", "", "", "", ""},
                {CARS, "SKODA", "OCTAVIA", "2017", "LIFTBACK", "", "", "", "", ""},
                {CARS, "SKODA", "OCTAVIA", "2017", "LIFTBACK", "9338208", "", "", "", ""},
                {CARS, "SKODA", "OCTAVIA", "2017", "LIFTBACK", "9338208", "LPG", "", "", ""},
                {CARS, "SKODA", "OCTAVIA", "2017", "LIFTBACK", "9338208", "LPG", "FORWARD_CONTROL", "", ""},
                {CARS, "SKODA", "OCTAVIA", "2017", "LIFTBACK", "9338208", "LPG", "FORWARD_CONTROL", "MECHANICAL", ""},
                {CARS, "SKODA", "OCTAVIA", "2017", "LIFTBACK", "9338208", "LPG", "FORWARD_CONTROL", "MECHANICAL", "21111104"},
        });
    }

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.catalog().suggestCatalog().categoryPath(category.name()).markQuery(mark).modelQuery(model).yearQuery(year)
                .bodyTypeQuery(bodyType).superGenIdQuery(superGenId).engineTypeQuery(engineType).transmissionQuery(transmission).gearTypeQuery(gearType).techParamIdQuery(techParamId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        JsonObject apiResult = request.apply(api);
        JsonObject prodResult = request.apply(prodApi);

        MatcherAssert.assertThat(apiResult, jsonEquals(prodResult).whenIgnoringPaths(
                "car_suggest.marks[*].offers_count",
                "car_suggest.models[*].offers_count"
        ));
    }
}
