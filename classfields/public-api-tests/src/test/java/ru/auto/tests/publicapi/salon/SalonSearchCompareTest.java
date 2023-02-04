package ru.auto.tests.publicapi.salon;

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
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.api.SalonApi.SalonSearchOper.PAGE_QUERY;
import static ru.auto.tests.publicapi.api.SalonApi.SalonSearchOper.PAGE_SIZE_QUERY;
import static ru.auto.tests.publicapi.api.SalonApi.SalonSearchOper.TARIFF_TYPE_QUERY;
import static ru.auto.tests.publicapi.model.AutoApiSearchSalonSearchRequestParameters.TariffTypeEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiSearchSalonSearchRequestParameters.TariffTypeEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiSearchSalonSearchRequestParameters.TariffTypeEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Generated compare test for SalonSearch
 */
@DisplayName("GET /salon/search")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SalonSearchCompareTest {

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
        return newArrayList(
                req -> req.addQueryParam(TARIFF_TYPE_QUERY, CARS).addQueryParam(PAGE_QUERY, 1).addQueryParam(PAGE_SIZE_QUERY, 10),
                req -> req.addQueryParam(TARIFF_TYPE_QUERY, MOTO).addQueryParam(PAGE_QUERY, 1).addQueryParam(PAGE_SIZE_QUERY, 10),
                req -> req.addQueryParam(TARIFF_TYPE_QUERY, TRUCKS).addQueryParam(PAGE_QUERY, 1).addQueryParam(PAGE_SIZE_QUERY, 10)
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /salon/search/{category}")
    public void shouldSalonSearchHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.salon().salonSearch()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
