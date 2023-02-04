package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.ra.ResponseSpecBuilders;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;


@Title("GET /user/offers/<offerId>/edit")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class EditOfferTest {

    private static final String IGNORED_PATH = ".unifiedLocation";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.userOffers().getUserOfferForEditRoute().offerIdPath(getRandomOfferId()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.userOffers().getUserOfferForEditRoute().offerIdPath(getRandomOfferId()).reqSpec(authSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Issue("REALTY-13890")
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = getRandomOfferId();
        api.userOffers().getUserOfferForEditRoute().offerIdPath(offerId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(ResponseSpecBuilders.shouldBe404OfferNotFound(account.getId(), offerId)));
    }

    @Test
    public void shouldSee404WhenDeleteNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = getRandomOfferId();
        adaptor.vosUser(token);
        api.userOffers().getUserOfferForEditRoute().offerIdPath(offerId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(ResponseSpecBuilders.shouldBe404OfferNotFound(account.getId(), offerId)));
    }

    @Test
    public void shouldEditOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userOffers().getUserOfferForEditRoute()
                .offerIdPath(offerId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths(IGNORED_PATH));
    }
}
