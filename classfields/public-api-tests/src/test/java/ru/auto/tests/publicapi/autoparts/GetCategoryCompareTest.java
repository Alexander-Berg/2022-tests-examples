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
import ru.auto.tests.publicapi.api.AutopartsApi;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetCategoryOper.SECTION_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated compare test for GetCategory
 */
@DisplayName("GET /autoparts/category")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetCategoryCompareTest {

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
        return newArrayList(req -> req.addQueryParam(SECTION_QUERY, "auto")
                //   .addQueryParam(GetCategoryOper.CATEGORY_ID_QUERY, 56)
                //   .addQueryParam(GetCategoryOper.CONTEXT_QUERY, "context_example")
                //   .addQueryParam(GetCategoryOper.MARK_QUERY, "mark_example")
                //   .addQueryParam(GetCategoryOper.MODEL_QUERY, "model_example")
                //   .addQueryParam(GetCategoryOper.GENERATION_QUERY, "generation_example")
                //   .addQueryParam(GetCategoryOper.RGID_QUERY, 56)
                //   .addQueryParam(GetCategoryOper.GEO_RADIUS_QUERY, 56)
                //   .addQueryParam(GetCategoryOper.SHIPPING_QUERY, "shipping_example")
                //   .addQueryParam(GetCategoryOper.IS_NEW_QUERY, true)
                //   .addQueryParam(GetCategoryOper.IS_AVAILABLE_QUERY, true)
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /autoparts/category")
    public void shouldGetCategoryHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().getCategory()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).prettyPeek().as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
