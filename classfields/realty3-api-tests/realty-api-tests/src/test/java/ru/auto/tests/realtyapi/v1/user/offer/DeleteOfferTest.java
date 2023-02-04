package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import org.assertj.Assertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.ra.ResponseSpecBuilders;
import ru.auto.tests.realtyapi.responses.IdsResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404UnknownVosUser;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;


@Title("DELETE /user/offers/<offerId>")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class DeleteOfferTest {

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
        api.userOffers().deleteOfferRoute().offerIdPath(getRandomOfferId()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.userOffers().deleteOfferRoute().offerIdPath(getRandomOfferId()).reqSpec(authSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.userOffers().deleteOfferRoute().offerIdPath(getRandomOfferId())
                .reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBe404UnknownVosUser(account.getId())));
    }

    @Test
    @Issues({@Issue("REALTY-13891"), @Issue("REALTYBACK-1212")})
    @Ignore("REALTYBACK-1212")
    public void shouldSee404ForNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = getRandomOfferId();
        adaptor.vosUser(token);
        api.userOffers().deleteOfferRoute().offerIdPath(offerId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(ResponseSpecBuilders.shouldBe404OfferNotFound(account.getId(), offerId)));
    }

    @Test
    public void shouldDeleteOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();
        IdsResponse response = api.userOffers().deleteOfferRoute().offerIdPath(offerId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBeOK())).as(IdsResponse.class, GSON);
        Assertions.assertThat(response.getResponse()).hasOnlyIds(offerId);
    }
}
