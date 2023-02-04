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
import static ru.auto.tests.publicapi.api.AutopartsApi.SearchOper.CATEGORY_ID_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SearchOper.PRICE_FROM_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SearchOper.PRICE_TO_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SearchOper.SECTION_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated compare test for Search
 */
@DisplayName("GET /autoparts/offer/search")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SearchCompareTest {

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
                //   .addQueryParam(SearchOper.MARK_QUERY, "mark_example")
                //   .addQueryParam(SearchOper.MODEL_QUERY, "model_example")
                //   .addQueryParam(SearchOper.GENERATION_QUERY, "generation_example")
                //   .addQueryParam(SearchOper._CONFIGURATION_QUERY, "_configuration_example")
                //   .addQueryParam(SearchOper.TECH_PARAMS_QUERY, "techParams_example")
                   .addQueryParam(CATEGORY_ID_QUERY, 28091)
                //   .addQueryParam(SearchOper.EXACT_CATEGORY_QUERY, true)
                //   .addQueryParam(SearchOper.BRAND_ID_QUERY, Arrays.asList())
                //   .addQueryParam(SearchOper.BRAND_MODEL_ID_QUERY, Arrays.asList())
                //   .addQueryParam(SearchOper.OEM_QUERY, "oem_example")
//                   .addQueryParam(RGID_QUERY, 56)
//                   .addQueryParam(GEO_RADIUS_QUERY, 56)
                //   .addQueryParam(SearchOper.SHIPPING_QUERY, "shipping_example")
                //   .addQueryParam(SearchOper.ARE_PICTURES_REQUIRED_QUERY, true)
                //   .addQueryParam(SearchOper.IS_FROM_VOS_QUERY, true)
                //   .addQueryParam(SearchOper.SELLER_ID_QUERY, "sellerId_example")
                //   .addQueryParam(SearchOper.FEED_ID_QUERY, "feedId_example")
                //   .addQueryParam(SearchOper.SELLER_TYPE_QUERY, "sellerType_example")
                //   .addQueryParam(SearchOper.PAGE_QUERY, 56)
                //   .addQueryParam(SearchOper.PAGE_SIZE_QUERY, 5)
                //   .addQueryParam(SearchOper.SORT_QUERY, "sort_example")
                //   .addQueryParam(SearchOper.SORT_DIRECTION_QUERY, "sortDirection_example")
                //   .addQueryParam(SearchOper.IS_NEW_QUERY, true)
                //   .addQueryParam(SearchOper.IS_AVAILABLE_QUERY, true)
                //   .addQueryParam(SearchOper.TEXT_QUERY, "text_example")
                //   .addQueryParam(SearchOper.RENDER_MODERATION_QUERY, true)
                //   .addQueryParam(SearchOper.IS_FOR_PRIORITY_QUERY, true)
                //   .addQueryParam(SearchOper.STICKY_RANDOM_QUERY, 56L)
                //   .addQueryParam(SearchOper.WITHOUT_COUNTS_QUERY, true)
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /autoparts/offer/search")
    public void shouldSearchHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().search()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths("offers[*].encrypted_billing_dump"));
    }
}
