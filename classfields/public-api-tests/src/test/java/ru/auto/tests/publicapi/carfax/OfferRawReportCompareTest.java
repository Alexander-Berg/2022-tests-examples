package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.CARFAX;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /carfax/offer/{category}/{offerID}/raw")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class OfferRawReportCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(CARFAX)
    public void shouldNotBoughtOfferRawReportHasNoDiffWithProduction() {
        String offerId = RawReportUtils.createTestOffer(adaptor, am);

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().offerRawReport()
                .reqSpec(defaultSpec())
                .offerIDPath(offerId)
                .categoryPath(CARS)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS));
    }

    @Test
    @Owner(CARFAX)
    public void shouldNotBoughtOfferRawReportForOfferOwnerHasNoDiffWithProduction() {
        Account offerOwnerAccount = am.create();
        String offerOwnerSessionId = adaptor.login(offerOwnerAccount).getSession().getId();
        String offerId = RawReportUtils.createTestOffer(adaptor, offerOwnerAccount, offerOwnerSessionId);

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().offerRawReport()
                .reqSpec(defaultSpec())
                .offerIDPath(offerId)
                .xSessionIdHeader(offerOwnerSessionId)
                .categoryPath(CARS)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS));
    }

    @Test
    @Owner(CARFAX)
    public void shouldBoughtOfferRawReportHasNoDiffWithProduction() {
        String offerId = RawReportUtils.createTestOffer(adaptor, am);

        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.buyVinOfferHistory(sessionId, account.getId(), offerId);

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().offerRawReport()
                .reqSpec(defaultSpec())
                .offerIDPath(offerId)
                .categoryPath(CARS)
                .decrementQuotaQuery(true)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS));
    }
}
