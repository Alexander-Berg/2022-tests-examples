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
import static ru.auto.tests.publicapi.api.AutopartsApi.GetCategoriesOper.CATEGORY_ID_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
* Generated compare test for GetCategories
*/
@DisplayName("GET /autoparts/categories")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetCategoriesCompareTest {

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
        //   .addQueryParam(GetCategoriesOper.SELLER_ID_QUERY, "sellerId_example")
        //   .addQueryParam(GetCategoriesOper.MARK_QUERY, "mark_example")
        //   .addQueryParam(GetCategoriesOper.MODEL_QUERY, "model_example")
        //   .addQueryParam(GetCategoriesOper.GENERATION_QUERY, "generation_example")
        //   .addQueryParam(GetCategoriesOper.DEPTH_QUERY, 2)
        //   .addQueryParam(GetCategoriesOper.LAST_NODE_LIMIT_QUERY, 5)
        //   .addQueryParam(GetCategoriesOper.RGID_QUERY, 56)
        //   .addQueryParam(GetCategoriesOper.GEO_RADIUS_QUERY, 56)
        //   .addQueryParam(GetCategoriesOper.SHIPPING_QUERY, "shipping_example")
        //   .addQueryParam(GetCategoriesOper.IS_NEW_QUERY, true)
        //   .addQueryParam(GetCategoriesOper.IS_AVAILABLE_QUERY, true)
        //   .addQueryParam(GetCategoriesOper.SHOW_EMPTY_QUERY, false)
        //   .addQueryParam(GetCategoriesOper.CONTEXT_QUERY, "context_example")
);
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /autoparts/categories")
    public void shouldGetCategoriesHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().getCategories()
            .reqSpec(defaultSpec())
            .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
