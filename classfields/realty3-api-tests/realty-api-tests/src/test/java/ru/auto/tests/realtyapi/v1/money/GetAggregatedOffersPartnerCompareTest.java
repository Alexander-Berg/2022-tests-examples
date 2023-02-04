package ru.auto.tests.realtyapi.v1.money;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.restassured.builder.RequestSpecBuilder;
import org.assertj.core.api.Assertions;
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
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.environment.IntegrationTestEnvironment.MONEY_PARTNER_ID;
import static ru.auto.tests.realtyapi.environment.IntegrationTestEnvironment.MONEY_PARTNER_UID;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authUidSpec;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.getParametersForAggregatedOfferStatComparison;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /money/spent/aggregated/offers/partner/{partnerId}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetAggregatedOffersPartnerCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Prod
    @Inject
    private ApiClient prodApi;

    @Parameter
    @Parameterized.Parameter(0)
    public Consumer<RequestSpecBuilder> reqSpec;

    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return getParametersForAggregatedOfferStatComparison();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.money().partnerAggregatedWithOffersSpent()
                .reqSpec(authSpec())
                .reqSpec(authUidSpec(MONEY_PARTNER_UID))
                .reqSpec(reqSpec)
                .partnerIdPath(MONEY_PARTNER_ID)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        JsonObject response = request.apply(api);
        Assertions.assertThat(response.getAsJsonArray("response")).isNotEmpty();
        MatcherAssert.assertThat(response, jsonEquals(request.apply(prodApi)).when(IGNORING_ARRAY_ORDER));
    }
}
