package ru.auto.tests.publicapi.autoparts;

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
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOper.CATEGORY_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOper.GEO_RADIUS_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOper.IGNORE_STATS_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOper.ONLY_CATEGORIES_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOper.RGID_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOper.SHIPPING_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOper.TEXT_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated compare test for SuggestAutoParts
 */
@DisplayName("GET /autoparts/suggest")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SuggestAutoPartsCompareTest {

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
        return newArrayList(req -> req
                .addQueryParam(CATEGORY_QUERY, "category_example")
                .addQueryParam(TEXT_QUERY, "капот bmw")
                .addQueryParam(ONLY_CATEGORIES_QUERY, false)
                .addQueryParam(IGNORE_STATS_QUERY, true)
                .addQueryParam(RGID_QUERY, 56)
                .addQueryParam(GEO_RADIUS_QUERY, 56)
                .addQueryParam(SHIPPING_QUERY, "shipping_example"));
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /autoparts/suggest")
    public void shouldSuggestAutoPartsHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().suggestAutoParts().reqSpec(defaultSpec()).reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
