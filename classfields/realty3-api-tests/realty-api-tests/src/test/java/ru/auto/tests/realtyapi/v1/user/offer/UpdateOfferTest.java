package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import org.assertj.Assertions;
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
import ru.auto.tests.realtyapi.responses.IdResponse;
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404OfferNotFound;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;
import static ru.auto.tests.realtyapi.v1.testdata.OfferType.APARTMENT_SELL;


@Title("PUT /user/offers/<offerId>")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class UpdateOfferTest {

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

    @Inject
    private FtlProcessor ftlProcessor;


    @Test
    public void shouldSee403WithoutHeaders() {
        api.userOffers().updateOfferRoute().offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.userOffers().updateOfferRoute().offerIdPath(getRandomOfferId()).reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldNotUpdateOfferWithoutBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();

        api.userOffers().updateOfferRoute().offerIdPath(offerId).reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.userOffers().updateOfferRoute().offerIdPath(getRandomOfferId())
                .reqSpec(authSpec()).authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(APARTMENT_SELL.value())))
                .execute(validatedWith(
                        /* used to be a call to shouldBe404UnknownVosUser, but it is no longer proxying this response from VOS */
                        shouldBeCode(SC_NOT_FOUND)
                                .expectBody(ResponseSpecBuilders.ERROR_CODENAME, equalTo("UNKNOWN_VOS_USER"))
                                .expectBody(
                                        ResponseSpecBuilders.ERROR_DATA_DESCRIPTION,
                                        equalTo(String.format("User %s not found", account.getId()))
                                )
                ));
    }

    @Test
    @Issue("REALTY-13891")
    public void shouldSee404ForNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = getRandomOfferId();
        adaptor.vosUser(token);
        api.userOffers().updateOfferRoute().offerIdPath(offerId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(APARTMENT_SELL.value())))
                .execute(validatedWith(shouldBe404OfferNotFound(offerId)));
    }

    @Test
    public void shouldUpdateOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token, APARTMENT_SELL.value()).getResponse().getId();

        IdResponse response = api.userOffers().updateOfferRoute().offerIdPath(offerId).reqSpec(authSpec()).authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(APARTMENT_SELL.value())))
                .execute(validatedWith(shouldBeOK())).as(IdResponse.class, GSON);

        Assertions.assertThat(response.getResponse()).hasId(offerId);

    }
}
