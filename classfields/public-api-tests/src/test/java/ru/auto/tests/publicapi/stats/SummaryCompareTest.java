package ru.auto.tests.publicapi.stats;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
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
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.api.StatsApi.SummaryStatsOper.COMPLECTATION_ID_QUERY;
import static ru.auto.tests.publicapi.api.StatsApi.SummaryStatsOper.CONFIGURATION_ID_QUERY;
import static ru.auto.tests.publicapi.api.StatsApi.SummaryStatsOper.MARK_QUERY;
import static ru.auto.tests.publicapi.api.StatsApi.SummaryStatsOper.MODEL_QUERY;
import static ru.auto.tests.publicapi.api.StatsApi.SummaryStatsOper.SUPER_GEN_QUERY;
import static ru.auto.tests.publicapi.api.StatsApi.SummaryStatsOper.TECH_PARAM_ID_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /stats/summary")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SummaryCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Параметры")
    @Parameterized.Parameter
    public Consumer<RequestSpecBuilder> reqSpec;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return newArrayList(
                req -> req.addQueryParam(MARK_QUERY, getRandomString()),
                req -> req.addQueryParam(MARK_QUERY, "bmw"),
                req -> req.addQueryParam(MARK_QUERY, "bmw")
                        .addQueryParam(MODEL_QUERY, "5er").addQueryParam(SUPER_GEN_QUERY, "10436648")
                        .addQueryParam(CONFIGURATION_ID_QUERY, "10436649")
                        .addQueryParam(TECH_PARAM_ID_QUERY, "10436651")
                        .addQueryParam(COMPLECTATION_ID_QUERY, "20580971"));
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.stats().summaryStats().reqSpec(defaultSpec())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
