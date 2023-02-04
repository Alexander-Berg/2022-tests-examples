package ru.auto.tests.publicapi.favorites;

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
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400IncorrectOfferIdError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("DELETE /user/favorites/{category}/{offerId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteFromFavoritesTest {

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
        api.userFavorites().deleteFavorite().categoryPath(CARS.name()).offerIdPath(Utils.getRandomString()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeSuccessWithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        adaptor.addFavorites(sessionId, CARS, offerId);

        api.userFavorites().deleteFavorite().categoryPath(CARS.name()).offerIdPath(offerId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldDeleteFromFavoritesForAnonym() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String anonymSessionId = adaptor.session().getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        adaptor.addFavorites(anonymSessionId, CARS, offerId);

        api.userFavorites().deleteFavorite().categoryPath(CARS.name()).offerIdPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        adaptor.addFavorites(sessionId, CARS, offerId);
        String incorrectCategory = Utils.getRandomString();

        api.userFavorites().deleteFavorite().categoryPath(incorrectCategory).offerIdPath(offerId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    public void shouldSee400WithIncorrectOfferId() {
        String incorrectOfferId = Utils.getRandomString();
        api.userFavorites().deleteFavorite().categoryPath(CARS.name()).offerIdPath(incorrectOfferId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));
    }
}
