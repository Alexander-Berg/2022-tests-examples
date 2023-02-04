package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NeedAuthentication;

@DisplayName("GET /user/offers/{category}/mark-models")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class OfferMarkModelsNegativeTest {

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
        api.userOffers().markModels().categoryPath(CARS).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSessionId() {
        api.userOffers().markModels().categoryPath(CARS)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe401NeedAuthentication()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithIncorrectCategory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String incorrectCategory = Utils.getRandomString();
        api.userOffers().markModels().categoryPath(incorrectCategory)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }
}
