package ru.auto.tests.realtyapi.v1.primarysale;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.builder.RequestSpecBuilder;
import net.javacrumbs.jsonunit.core.Option;
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

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.environment.IntegrationTestEnvironment.SITE_ID_WITH_PRIMARY_SALE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.api.OfferStatApi.PrimarySaleRoomsStatRouteOper.DELIVERY_DATE_QUERY;
import static ru.auto.tests.realtyapi.v1.api.OfferStatApi.PrimarySaleRoomsStatRouteOper.LAST_FLOOR_QUERY;
import static ru.auto.tests.realtyapi.v1.api.OfferStatApi.PrimarySaleRoomsStatRouteOper.SITE_ID_QUERY;

/**
 * Generated compare test for PrimarySaleRoomsStatRoute
 */
@DisplayName("GET /primarySale/roomsStat")
@GuiceModules(RealtyApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PrimarySaleRoomsStatRouteTest {

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
        return newArrayList(req -> req.and().addQueryParam(SITE_ID_QUERY, SITE_ID_WITH_PRIMARY_SALE),
                req -> req.addQueryParam(SITE_ID_QUERY, SITE_ID_WITH_PRIMARY_SALE).addQueryParam(DELIVERY_DATE_QUERY, "2019_4"),
                req -> req.addQueryParam(SITE_ID_QUERY, SITE_ID_WITH_PRIMARY_SALE).addQueryParam(DELIVERY_DATE_QUERY, "2021_1"),
                req -> req.addQueryParam(SITE_ID_QUERY, SITE_ID_WITH_PRIMARY_SALE).addQueryParam(DELIVERY_DATE_QUERY, "finished"),
                //last floor tests:
                req -> req.addQueryParam(SITE_ID_QUERY, SITE_ID_WITH_PRIMARY_SALE).addQueryParam(LAST_FLOOR_QUERY, true),
                req -> req.addQueryParam(SITE_ID_QUERY, SITE_ID_WITH_PRIMARY_SALE).addQueryParam(LAST_FLOOR_QUERY, false)
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /primarySale/roomsStat")
    public void shouldPrimarySaleRoomsStatRouteHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.offerStat().primarySaleRoomsStatRoute()
                .reqSpec(authSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
            .when(Option.IGNORING_ARRAY_ORDER));
    }
}
