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
import static ru.auto.tests.publicapi.api.AutopartsApi.SimilarOper.CATEGORY_ID_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SimilarOper.COUNT_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated compare test for Similar
 */
@DisplayName("GET /autoparts/similar")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SimilarCompareTest {

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
                   .addQueryParam(CATEGORY_ID_QUERY, 56)
                //   .addQueryParam(SimilarOper.MARK_QUERY, "mark_example")
                //   .addQueryParam(SimilarOper.MODEL_QUERY, "model_example")
                //   .addQueryParam(SimilarOper.GENERATION_QUERY, "generation_example")
                //   .addQueryParam(SimilarOper._CONFIGURATION_QUERY, "_configuration_example")
                //   .addQueryParam(SimilarOper.TECH_PARAMS_QUERY, "techParams_example")
                //   .addQueryParam(SimilarOper.RGID_QUERY, 56)
                //   .addQueryParam(SimilarOper.GEO_RADIUS_QUERY, 56)
                //   .addQueryParam(SimilarOper.SHIPPING_QUERY, "shipping_example")
                //   .addQueryParam(SimilarOper.SELLER_ID_QUERY, "sellerId_example")
                //   .addQueryParam(SimilarOper.SELLER_ID2_QUERY, "sellerId_example")
                //   .addQueryParam(SimilarOper.EXCLUDE_SELLER_QUERY, false)
                   .addQueryParam(COUNT_QUERY, 1)
                //   .addQueryParam(SimilarOper.ID_QUERY, "id_example")
                //   .addQueryParam(SimilarOper.SORT_QUERY, "sort_example")
                //   .addQueryParam(SimilarOper.IS_NEW_QUERY, true)
                //   .addQueryParam(SimilarOper.CONTEXT_QUERY, "context_example")
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /autoparts/similar")
    public void shouldSimilarHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().similar()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths("offers[*].encrypted_billing_dump"));
    }
}
