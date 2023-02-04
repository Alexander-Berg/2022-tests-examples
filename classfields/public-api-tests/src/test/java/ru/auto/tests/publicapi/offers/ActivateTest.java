package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplate;
import ru.auto.tests.publicapi.model.AutoApiActivationResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400IncorrectOfferIdError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NeedAuthentication;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("POST /user/offers/{category}/{offerID}/activate")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class ActivateTest {
    private static final int POLL_INTERVAL = 2;
    private static final int TIMEOUT = 30;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userOffers().activate().categoryPath(CARS.name()).offerIDPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        api.userOffers().activate().categoryPath(CARS.name()).offerIDPath(offerId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NeedAuthentication()));
    }

    @Test
    public void shouldSee400WithIncorrectOfferId() {
        String incorrectOfferId = Utils.getRandomString();
        api.userOffers().activate().categoryPath(CARS.name()).offerIDPath(incorrectOfferId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String incorrectCategory = Utils.getRandomString();
        api.userOffers().activate().categoryPath(incorrectCategory).offerIDPath(offerId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Ignore("Попробовать активировать неактивный оффер")
    @Description("Для cars объявление активируется через 30 секунд")
    public void shouldActivateOffer() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.userOffers().activate().categoryPath(CARS.name()).offerIDPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Description("Должны увидеть похожий оффер")
    public void shouldSeeSimilarOffer() {
        String draft = "offers/cars_hidden_offer.ftl";
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String testOffer = new OfferTemplate().process(draft, account.getLogin());
        String firstDraftId = adaptor.createDraft(sessionId, CARS, testOffer).getOfferId();
        String firstOfferId = adaptor.publishDraft(sessionId, CARS, firstDraftId).getOfferId();

        String secondDraftId = adaptor.createDraft(sessionId, CARS, testOffer).getOfferId();
        String secondOfferId = adaptor.publishDraft(sessionId, CARS, secondDraftId).getOfferId();


        AutoApiActivationResponse response = api.userOffers().activate().categoryPath(CARS)
                .offerIDPath(secondOfferId)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_PAYMENT_REQUIRED)))
                .as(AutoApiActivationResponse.class);

        assertThat(response.getSimilarOffer().getId()).isEqualTo(firstOfferId);
        assertThat(response.getPriceInfo().getPrice()).isGreaterThan(0);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeFreeLimit() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createOffer(account.getLogin(), sessionId, CARS);

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS, "offers/cars_bmw_3er_hidden.ftl").getOfferId();

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> api.userOffers().getMyOffer().categoryPath(CARS).offerIDPath(offerId).xSessionIdHeader(sessionId)
                        .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()))
                        .getOffer().getServicePrices().stream().filter(item -> item.getService()
                                .equals("all_sale_activate")).findFirst().get().getPrice(), is(not(equalTo(0))));

        AutoApiActivationResponse response = api.userOffers().activate().categoryPath(CARS)
                .offerIDPath(offerId)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_PAYMENT_REQUIRED)))
                .as(AutoApiActivationResponse.class);

        assertThat(response.getPriceInfo().getPrice()).isGreaterThan(0);
        assertThat(response.getPaidReason()).isEqualTo(AutoApiActivationResponse.PaidReasonEnum.FREE_LIMIT);
    }
}
