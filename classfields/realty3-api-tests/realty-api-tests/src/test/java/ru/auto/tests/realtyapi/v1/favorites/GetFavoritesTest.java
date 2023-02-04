package ru.auto.tests.realtyapi.v1.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import org.assertj.Assertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.FavoritesResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.HOUSE;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.RENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;


@Title("GET /favorites.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetFavoritesTest {

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
    @Issue("REALTY-13817")
    public void shouldSee403WithoutHeaders() {
        api.favorites().findFavoritesRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee200WithoutOAuth() {
        api.favorites().findFavoritesRoute().reqSpec(authSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    @Ignore("REALTY-13886")
    public void shouldSee400WithInvalidType() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.favorites().findFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .typeQuery(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Ignore("REALTY-13886")
    public void shouldSee400WithInvalidCategory() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.favorites().findFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .categoryQuery(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSeeEmptyFavorites() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        FavoritesResponse favorites = api.favorites().findFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(FavoritesResponse.class, GSON);

        Assertions.assertThat(favorites.getResponse()).hasNoActualField().hasNoOutdated().hasNoRelevant();
    }

    @Test
    @Ignore("Создавать офферы, которые будут актуальны")
    public void shouldSeeAllFavorites() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();
        String anotherOfferId = adaptor.getOfferIdFromSearcher();
        adaptor.addFavorites(token, offerId);
        adaptor.addFavorites(token, anotherOfferId);

        FavoritesResponse favorites = api.favorites().findFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(FavoritesResponse.class, GSON);

        Assertions.assertThat(favorites.getResponse()).hasNoActualField()
                .hasOutdated(offerId, anotherOfferId).hasNoRelevant();
    }

    @Test
    @Ignore("Создавать офферы, которые будут актуальны [REALTY-13886]")
    public void shouldSeeFavoritesOnlyWithTypeAndCategory() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();
        String anotherOfferId = adaptor.getOfferIdFromSearcher();
        adaptor.addFavorites(token, SELL, APARTMENT, offerId);
        adaptor.addFavorites(token, RENT, HOUSE, anotherOfferId);

        FavoritesResponse favorites = api.favorites().findFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .typeQuery(SELL.name()).categoryQuery(APARTMENT.name())
                .execute(validatedWith(shouldBe200Ok()))
                .as(FavoritesResponse.class, GSON);

        Assertions.assertThat(favorites.getResponse()).hasNoActualField()
                .hasOutdated(offerId).hasNoRelevant();
    }
}
