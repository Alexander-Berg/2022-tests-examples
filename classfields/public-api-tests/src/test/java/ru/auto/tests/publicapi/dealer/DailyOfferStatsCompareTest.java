package ru.auto.tests.publicapi.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
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
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.utils.UtilsPublicApi;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.api.DealerApi.GetOfferProductActivationsDailyStatsDealerOper.PRODUCT_PATH;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getBMWEurosibAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("GET /dealer/wallet/product/{product}/activations/offer-stats")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DailyOfferStatsCompareTest {
    private static final int DAYS_AGO = 7;
    private static final int DEFAULT_PAGE_SIZE = 50;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Аккаунт")
    @Parameterized.Parameter(0)
    public Account account;

    @Parameter("Параметры")
    @Parameterized.Parameter(1)
    public Consumer<RequestSpecBuilder> reqSpec;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {getBMWEurosibAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "placement")},
                {getBMWEurosibAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "boost")},
                {getBMWEurosibAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "premium")},
                {getBMWEurosibAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "special-offer")},
                {getBMWEurosibAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "badge")},
                {getDemoAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "placement")},
                {getDemoAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "boost")},
                {getDemoAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "premium")},
                {getDemoAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "special-offer")},
                {getDemoAccount(), (Consumer<RequestSpecBuilder>) req -> req.addPathParam(PRODUCT_PATH, "badge")}});
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldGetDailyOfferStatsHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.dealer()
                .getOfferProductActivationsDailyStatsDealer().pageSizeQuery(DEFAULT_PAGE_SIZE)
                .reqSpec(reqSpec).dateQuery(UtilsPublicApi.getTimeDaysAgo(DAYS_AGO))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
