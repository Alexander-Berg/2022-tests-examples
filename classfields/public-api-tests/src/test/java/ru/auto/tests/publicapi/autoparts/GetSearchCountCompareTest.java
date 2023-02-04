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
import static ru.auto.tests.publicapi.api.AutopartsApi.GetSearchCountOper.PRICE_FROM_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetSearchCountOper.PRICE_TO_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.GetSearchCountOper.SECTION_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated compare test for GetSearchCount
 */
@DisplayName("GET /autoparts/search-count")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSearchCountCompareTest {

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
                        .addQueryParam(SECTION_QUERY, "auto")
                        .addQueryParam(PRICE_FROM_QUERY, 56)
                        .addQueryParam(PRICE_TO_QUERY, 56)
                //   .addQueryParam(GetSearchCountOper.MARK_QUERY, "mark_example")
                //   .addQueryParam(GetSearchCountOper.MODEL_QUERY, "model_example")
                //   .addQueryParam(GetSearchCountOper.GENERATION_QUERY, "generation_example")
                //   .addQueryParam(GetSearchCountOper._CONFIGURATION_QUERY, "_configuration_example")
                //   .addQueryParam(GetSearchCountOper.TECH_PARAMS_QUERY, "techParams_example")
                //   .addQueryParam(GetSearchCountOper.CATEGORY_ID_QUERY, 56)
                //   .addQueryParam(GetSearchCountOper.EXACT_CATEGORY_QUERY, true)
                //   .addQueryParam(GetSearchCountOper.BRAND_ID_QUERY, 56)
                //   .addQueryParam(GetSearchCountOper.OEM_QUERY, "oem_example")
                //   .addQueryParam(GetSearchCountOper.RGID_QUERY, 56)
                //   .addQueryParam(GetSearchCountOper.GEO_RADIUS_QUERY, 56)
                //   .addQueryParam(GetSearchCountOper.SHIPPING_QUERY, "shipping_example")
                //   .addQueryParam(GetSearchCountOper.IS_FROM_VOS_QUERY, true)
                //   .addQueryParam(GetSearchCountOper.SELLER_ID_QUERY, "sellerId_example")
                //   .addQueryParam(GetSearchCountOper.FEED_ID_QUERY, "feedId_example")
                //   .addQueryParam(GetSearchCountOper.SELLER_TYPE_QUERY, "sellerType_example")
                //   .addQueryParam(GetSearchCountOper.PAGE_QUERY, 56)
                //   .addQueryParam(GetSearchCountOper.PAGE_SIZE_QUERY, 56)
                //   .addQueryParam(GetSearchCountOper.SORT_QUERY, "sort_example")
                //   .addQueryParam(GetSearchCountOper.IS_NEW_QUERY, true)
                //   .addQueryParam(GetSearchCountOper.IS_AVAILABLE_QUERY, true)
                //   .addQueryParam(GetSearchCountOper.ID_QUERY, "id_example")
                //   .addQueryParam(GetSearchCountOper.TEXT_QUERY, "text_example")
                //   .addQueryParam(GetSearchCountOper.STATUS_QUERY, "status_example")
                //   .addQueryParam(GetSearchCountOper.IS_HIDDEN_QUERY, true)
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /autoparts/search-count")
    public void shouldGetSearchCountHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().getSearchCount()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
