package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NeedAuthentication;

@DisplayName("GET /user/offers/{category}/{offerID}/stats")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class OfferStatsNegativeTest {

    private static final String DATE_FROM_FIELD_VALUE = "2019-09-01";
    private static final String DATE_TO_FIELD_VALUE = "2019-09-30";
    private static final String DATE_FROM_FIELD_ERROR = "Request is missing required query parameter 'from'";
    private static final String DATE_TO_FIELD_ERROR = "Request is missing required query parameter 'to'";

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
    @Owner(TIMONDL)
    public void shouldSee403WhenNoAuth() {
        api.userOffers().userOfferStat().categoryPath(CARS)
                .offerIDPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.userOffers().userOfferStat().categoryPath(CARS)
                .offerIDPath(offerId)
                .reqSpec(defaultSpec())
                .fromQuery(DATE_FROM_FIELD_VALUE)
                .toQuery(DATE_TO_FIELD_VALUE)
                .execute(validatedWith(shouldBe401NeedAuthentication()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithIncorrectCategory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String incorrectCategory = Utils.getRandomString();

        api.userOffers().userOfferStat().categoryPath(incorrectCategory)
                .offerIDPath(offerId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutDateFrom() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        AutoApiErrorResponse errorResponse = api.userOffers().userOfferStat().categoryPath(CARS)
                .offerIDPath(offerId)
                .reqSpec(defaultSpec())
                .toQuery(DATE_TO_FIELD_VALUE)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class);

        assertThat(errorResponse).hasStatus(ERROR).hasError(BAD_REQUEST);
        Assertions.assertThat(errorResponse.getDetailedError()).contains(DATE_FROM_FIELD_ERROR);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutDateTo() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        AutoApiErrorResponse errorResponse = api.userOffers().userOfferStat().categoryPath(CARS)
                .offerIDPath(offerId)
                .reqSpec(defaultSpec())
                .fromQuery(DATE_FROM_FIELD_VALUE)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class);

        assertThat(errorResponse).hasStatus(ERROR).hasError(BAD_REQUEST);
        Assertions.assertThat(errorResponse.getDetailedError()).contains(DATE_TO_FIELD_ERROR);
    }
}
