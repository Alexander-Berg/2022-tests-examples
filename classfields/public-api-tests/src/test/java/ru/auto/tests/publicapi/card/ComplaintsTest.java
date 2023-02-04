package ru.auto.tests.publicapi.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiComplaintRequest;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.ANOTHER;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("POST /offer/{category}/{offerID}/complaints")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class ComplaintsTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.offerCard().createComplaintOfferCard().categoryPath(CARS).offerIDPath(Utils.getRandomString())
                .body(new AutoApiComplaintRequest().reason(ANOTHER)).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithIncorrectOfferId() {
        api.offerCard().createComplaintOfferCard().categoryPath(CARS).offerIDPath(Utils.getRandomString()).reqSpec(defaultSpec())
                .body(new AutoApiComplaintRequest().reason(ANOTHER)).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.offerCard().createComplaintOfferCard().categoryPath(CARS).offerIDPath(offerId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithNoTextForAnotherReason() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        AutoApiErrorResponse response = api.offerCard().createComplaintOfferCard().categoryPath(CARS).offerIDPath(offerId).reqSpec(defaultSpec())
                .body(new AutoApiComplaintRequest().reason(ANOTHER)).execute(validatedWith(ResponseSpecBuilders.shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);

        assertThat(response).hasDetailedError("Text can not be empty when reason is ANOTHER");
    }
}
