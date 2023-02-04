package ru.auto.tests.realtyapi.v1.suggest;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.builder.RequestSpecBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.api.SuggestApi;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.api.SuggestApi.SuggestDeveloperRouteOper.MAX_COUNT_QUERY;
import static ru.auto.tests.realtyapi.v1.api.SuggestApi.SuggestDeveloperRouteOper.RGID_QUERY;
import static ru.auto.tests.realtyapi.v1.api.SuggestApi.SuggestDeveloperRouteOper.TEXT_QUERY;

/**
 * Generated compare test for SuggestDeveloperRoute
 */
@DisplayName("GET /suggest/developer")
@GuiceModules(RealtyApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SuggestDeveloperRouteCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter
    public Consumer<RequestSpecBuilder> reqSpec;

    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return newArrayList(req -> req.and()
                .addQueryParam(TEXT_QUERY, "пи")
                .addQueryParam(RGID_QUERY, 741964)
                .addQueryParam(MAX_COUNT_QUERY, 10)

        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /suggest/developer")
    public void shouldSuggestDeveloperRouteHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.suggest().suggestDeveloperRoute()
                .reqSpec(authSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
