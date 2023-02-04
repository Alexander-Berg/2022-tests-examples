package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOfferHideRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOfferHideRequest.ReasonEnum.SOLD_ON_AUTORU;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400IncorrectOfferIdError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NeedAuthentication;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("POST /user/offers/{category}/{offerID}/hide")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class HideTest {

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
        api.userOffers().hide().categoryPath(CARS.name()).offerIDPath(Utils.getRandomString()).body(getOfferHideRequest()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        api.userOffers().hide().categoryPath(CARS.name()).offerIDPath(offerId)
                .reqSpec(defaultSpec()).body(getOfferHideRequest()).execute(validatedWith(shouldBe401NeedAuthentication()));
    }

    @Test
    public void shouldSee400WithIncorrectOfferId() {
        String incorrectOfferId = Utils.getRandomString();
        api.userOffers().hide().categoryPath(CARS.name()).offerIDPath(incorrectOfferId)
                .reqSpec(defaultSpec()).body(getOfferHideRequest()).execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String incorrectCategory = Utils.getRandomString();
        api.userOffers().hide().categoryPath(incorrectCategory).offerIDPath(offerId)
                .reqSpec(defaultSpec()).body(getOfferHideRequest())
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.userOffers().hide().categoryPath(CARS.name()).offerIDPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    private static AutoApiOfferHideRequest getOfferHideRequest() {
        return new AutoApiOfferHideRequest().manySpamCalls(true).reason(SOLD_ON_AUTORU).soldPrice(400000);
    }
}
