package ru.auto.tests.publicapi.geo;


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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.api.GeoApi;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.function.Function.identity;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.api.GeoApi.SuggestGeoOper.LAT_QUERY;
import static ru.auto.tests.publicapi.api.GeoApi.SuggestGeoOper.LETTERS_QUERY;
import static ru.auto.tests.publicapi.api.GeoApi.SuggestGeoOper.LON_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /geo/suggest")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SuggestCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Параметры")
    @Parameterized.Parameter
    public Consumer<RequestSpecBuilder> reqSpec;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return newArrayList(
                req -> identity(),
                req -> req.addQueryParam(LETTERS_QUERY, "Кириши"),
                req -> req.addQueryParam(LETTERS_QUERY, "Кириши").addQueryParam(GeoApi.SuggestGeoOper.ONLY_CITIES_QUERY, true),
                req -> req.addQueryParam(LON_QUERY, "59.9342802").addQueryParam(LAT_QUERY, "30.335098600000038"),
                req -> req.addQueryParam(LETTERS_QUERY, Utils.getRandomString())
        );
    }

    @Test
    public void shouldGeoNoHasDifferenceWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.geo().suggestGeo().reqSpec(defaultSpec())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
