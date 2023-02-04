package ru.auto.tests.realtyapi.v1.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404OfferNotFound;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404UnknownVosUser;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;
import static ru.auto.tests.realtyapi.v1.testdata.OfferType.APARTMENT_SELL;


@Title("PUT /user/offers/draft/<id>")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class UpdateDraftTest {

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
        api.draft().updateDraftRoute().offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.draft().updateDraftRoute().reqSpec(authSpec()).offerIdPath(getRandomOfferId()).reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.draft().updateDraftRoute().offerIdPath(getRandomOfferId())
                .reqSpec(authSpec()).authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(APARTMENT_SELL.value())))
                .execute(validatedWith(shouldBe404UnknownVosUser(account.getId())));
    }

    @Test
    @Issue("REALTY-13891")
    public void shouldSee404ForNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        String offerId = getRandomOfferId();
        adaptor.vosUser(token);
        api.draft().updateDraftRoute().offerIdPath(offerId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(APARTMENT_SELL.value())))
                .execute(validatedWith(shouldBe404OfferNotFound(offerId)));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createEmptyDraft(token).getResponse().getId();

        api.draft().updateDraftRoute().offerIdPath(offerId).reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
