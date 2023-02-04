package ru.auto.tests.publicapi.card;


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
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiCallbackPhoneCallbackRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.CALLBACK_DISABLED;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("POST /offer/{category}/{offerID}/register-callback")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class RegisterCallbackTest {


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.offerCard().registerPhoneCallbackOfferCard().categoryPath(CARS)
                .offerIDPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.createOffer(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS).getOfferId();

        api.offerCard().registerPhoneCallbackOfferCard().categoryPath(CARS)
                .offerIDPath(id)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee404WithNotExistOffer() {
        String invalidOffer = Utils.getRandomString();
        api.offerCard().registerPhoneCallbackOfferCard().categoryPath(CARS).offerIDPath(invalidOffer)
                .body(new AutoApiCallbackPhoneCallbackRequest().phone(getRandomPhone()))
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldNotRegisterIfCallbackDisabled() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.createOffer(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS).getOfferId();

        AutoApiErrorResponse response = api.offerCard().registerPhoneCallbackOfferCard().categoryPath(AutoApiOffer.CategoryEnum.CARS).offerIDPath(id)
                .body(new AutoApiCallbackPhoneCallbackRequest().phone(Utils.getRandomPhone()))
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(ERROR)
                .hasError(CALLBACK_DISABLED)
                .hasDetailedError("Phone callback is disabled for given offer");
    }
}
