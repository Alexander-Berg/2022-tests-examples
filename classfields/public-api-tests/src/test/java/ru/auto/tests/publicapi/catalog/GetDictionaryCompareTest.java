package ru.auto.tests.publicapi.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.builder.RequestSpecBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.api.CatalogApi.DictionaryOper.CATEGORY_PATH;
import static ru.auto.tests.publicapi.api.CatalogApi.DictionaryOper.DICTIONARY_PATH;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultDictionaryFormatPath;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultDictionaryPaths;


@DisplayName("GET /reference/catalog/{category}/dictionaries/{format}/{dictionary}")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetDictionaryCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Параметры запроса")
    @Parameterized.Parameter
    public Consumer<RequestSpecBuilder> reqSpec;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return newArrayList(withDefaultDictionaryPaths(),
                getCarsReqSpec("drive"),
                getCarsReqSpec("body_type"),
                getCarsReqSpec("engine_type"),
                getCarsReqSpec("steering_wheel"),
                getCarsReqSpec("equipment"),
                getCarsReqSpec("transmission"),
                getCarsReqSpec("color_hex"),
                getCarsReqSpec("car_part"),
                getCarsReqSpec("damage_type"),

                getMotoReqSpec("cylinder_order"),
                getMotoReqSpec("moto_category"),
                getMotoReqSpec("stroke_amount"),
                getMotoReqSpec("snowmobile_type"),
                getMotoReqSpec("atv_type"),
                getMotoReqSpec("gear"),
                getMotoReqSpec("equipment"),
                getMotoReqSpec("moto_type"),
                getMotoReqSpec("transmission"),
                getMotoReqSpec("color_hex"),
                getMotoReqSpec("cylinder_amount"),
                getMotoReqSpec("engine"),

                getTrucksReqSpec("body_type"),
                getTrucksReqSpec("autoloader_type"),
                getTrucksReqSpec("chassis_suspension"),
                getTrucksReqSpec("truck_category"),
                getTrucksReqSpec("cabin_suspension"),
                getTrucksReqSpec("cabin"),
                getTrucksReqSpec("light_truck_type"),
                getTrucksReqSpec("trailer_type"),
                getTrucksReqSpec("gear"),
                getTrucksReqSpec("bus_type"),
                getTrucksReqSpec("wheel_drive"),
                getTrucksReqSpec("steering_wheel"),
                getTrucksReqSpec("euro_class"),
                getTrucksReqSpec("truck_type"),
                getTrucksReqSpec("swap_body_type"),
                getTrucksReqSpec("equipment"),
                getTrucksReqSpec("saddle_height"),
                getTrucksReqSpec("brakes"),
                getTrucksReqSpec("transmission"),
                getTrucksReqSpec("color_hex"),
                getTrucksReqSpec("suspension"),
                getTrucksReqSpec("engine"),

                getAllReqSpec("message_presets"),
                getAllReqSpec("message_hello_presets")
//                getAllReqSpec( "geo_suggest_listing")
        );
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.catalog().dictionary()
                .reqSpec(defaultSpec()).reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

    private static Consumer<RequestSpecBuilder> getCarsReqSpec(String dictionary) {
        return withDefaultDictionaryFormatPath().andThen(req -> req
                .addPathParam(CATEGORY_PATH, CARS)
                .addPathParam(DICTIONARY_PATH, dictionary));
    }

    private static Consumer<RequestSpecBuilder> getMotoReqSpec(String dictionary) {
        return withDefaultDictionaryFormatPath().andThen(req -> req
                .addPathParam(CATEGORY_PATH, MOTO)
                .addPathParam(DICTIONARY_PATH, dictionary));
    }

    private static Consumer<RequestSpecBuilder> getTrucksReqSpec(String dictionary) {
        return withDefaultDictionaryFormatPath().andThen(req -> req
                .addPathParam(CATEGORY_PATH, TRUCKS)
                .addPathParam(DICTIONARY_PATH, dictionary));
    }

    private static Consumer<RequestSpecBuilder> getAllReqSpec(String dictionary) {
        return withDefaultDictionaryFormatPath().andThen(req -> req
                .addPathParam(CATEGORY_PATH, "all")
                .addPathParam(DICTIONARY_PATH, dictionary));
    }
}
