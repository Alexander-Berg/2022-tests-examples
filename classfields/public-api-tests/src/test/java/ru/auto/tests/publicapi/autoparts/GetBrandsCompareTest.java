package ru.auto.tests.publicapi.autoparts;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.builder.RequestSpecBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Ignore;
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
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.BRAND_ID_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.BRAND_MODEL_ID_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.CATEGORY_ID_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.FILTER_BRAND_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.GEO_RADIUS_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.IS_AVAILABLE_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.IS_NEW_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetBrandsOper.RGID_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Generated compare test for GetBrands
 */
@DisplayName("GET /autoparts/brands")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetBrandsCompareTest {

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
                        .addQueryParam(BRAND_ID_QUERY, 56)
                        .addQueryParam(BRAND_MODEL_ID_QUERY, 56)
                        .addQueryParam(FILTER_BRAND_QUERY, true)
                        .addQueryParam(IS_NEW_QUERY, true)
                        .addQueryParam(IS_AVAILABLE_QUERY, true)
                        .addQueryParam(RGID_QUERY, 56)
                        .addQueryParam(GEO_RADIUS_QUERY, 56)
//           .addQueryParam(SHIPPING_QUERY, "shipping_example")
//           .addQueryParam(CONTEXT_QUERY, "context_example")
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /autoparts/brands")
    public void shouldGetBrandsHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().getBrands()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
