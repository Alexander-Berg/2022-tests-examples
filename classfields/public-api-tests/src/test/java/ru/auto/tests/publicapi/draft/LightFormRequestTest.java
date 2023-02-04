package ru.auto.tests.publicapi.draft;

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
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.lang.String.valueOf;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomDraftId;

@DisplayName("POST /user/draft/{category}/{offerId}/light-for-request")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class LightFormRequestTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee200() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();
        AutoApiDraftResponse draftResponse = adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);
        api.draft().sendLightOfferEvent().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftResponse.getOfferId())
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSee403WhenNoAuth() {
        api.draft().sendLightOfferEvent().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(valueOf(Utils.getRandomShortInt()))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutSession() {
        VertisPassportSession session = adaptor.session().getSession();
        String anonymSessionId = session.getId();
        api.draft().sendLightOfferEvent().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(valueOf(getRandomDraftId()))
                .reqSpec(defaultSpec())
                .xSessionIdHeader(anonymSessionId)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
