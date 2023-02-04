package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.ExamplePriceModel;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404OfferNotFound;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404UnknownVosUser;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;


@Title("PUT /user/offers/{offerId}/update_price")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class UpdatePriceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.userOffers().updatePriceRoute().offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.userOffers().updatePriceRoute().offerIdPath(getRandomOfferId()).reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.userOffers().updatePriceRoute().offerIdPath(getRandomOfferId())
                .reqSpec(authSpec()).authorizationHeader(token)
                .body(random(ExamplePriceModel.class))
                .execute(validatedWith(shouldBe404UnknownVosUser(account.getId())));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = getRandomOfferId();
        adaptor.vosUser(token);
        api.userOffers().updatePriceRoute().offerIdPath(offerId).reqSpec(authSpec()).authorizationHeader(token)
                .body(random(ExamplePriceModel.class))
                .execute(validatedWith(shouldBe404OfferNotFound(offerId)));
    }
}
