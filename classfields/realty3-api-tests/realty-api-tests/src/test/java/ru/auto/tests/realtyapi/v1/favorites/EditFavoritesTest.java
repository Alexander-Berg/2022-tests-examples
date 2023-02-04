package ru.auto.tests.realtyapi.v1.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
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
import ru.auto.tests.realtyapi.v1.model.FavoritesPatchRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.HOUSE;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.RENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;


@Title("PATCH /favorites.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class EditFavoritesTest {

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
        api.favorites().patchFavoritesRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Issue("REALTYBACK-1184")
    public void shouldSee400WithoutOAuth() {
        api.favorites().patchFavoritesRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithoutParams() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.favorites().patchFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Ignore("REALTY-13886")
    public void shouldSee400WithInvalidType() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.favorites().patchFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .typeQuery(Utils.getRandomString())
                .body(new FavoritesPatchRequest()
                        .add(newArrayList())
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Ignore("REALTY-13886")
    public void shouldSee400WithInvalidCategory() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.favorites().patchFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .categoryQuery(Utils.getRandomString())
                .body(new FavoritesPatchRequest()
                        .add(newArrayList())
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldEditWithEmptyBodyFavorites() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        FavoritesResponse favorites = api.favorites().patchFavoritesRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(favorites.getResponse()).hasNoActualField().hasNoOutdated().hasNoRelevant();
    }

    @Test
    public void shouldRemoveCategoryAndType() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();

        FavoritesResponse favorites = api.favorites().patchFavoritesRoute()
                .categoryQuery(APARTMENT.name())
                .typeQuery(SELL.name())
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList(offerId))
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(favorites.getResponse()).hasActualField(offerId).hasNoOutdated().hasRelevant(offerId);

        FavoritesResponse favoritesAfterDelete = api.favorites().patchFavoritesRoute()
                .categoryQuery(APARTMENT.name())
                .typeQuery(SELL.name())
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList())
                        .remove(newArrayList(offerId)))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(favoritesAfterDelete.getResponse()).hasNoActualField().hasOutdated().hasNoRelevant();
    }

    @Test
    @Ignore("REALTY-13886")
    public void shouldNotRemoveAnotherCategoryAndType() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();

        FavoritesResponse favorites = api.favorites().patchFavoritesRoute().categoryQuery(APARTMENT.name())
                .typeQuery(SELL.name())
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList(offerId))
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(favorites.getResponse()).hasNoActualField().hasOutdated(offerId).hasNoRelevant();

        FavoritesResponse favoritesAfterDelete = api.favorites().patchFavoritesRoute().categoryQuery(HOUSE.name())
                .typeQuery(RENT.name())
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList())
                        .remove(newArrayList(offerId)))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(favoritesAfterDelete.getResponse()).hasNoActualField().hasOutdated(offerId).hasNoRelevant();
    }

    @Test
    public void shouldTwiceAddToFavorites() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();
        String anotherOfferId = adaptor.getOfferIdFromSearcher();

        FavoritesResponse favorites = api.favorites().patchFavoritesRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList(offerId))
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(favorites.getResponse()).hasActualField(offerId).hasNoOutdated().hasRelevant(offerId);

        FavoritesResponse anotherFavorites = api.favorites().patchFavoritesRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList(anotherOfferId))
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(anotherFavorites.getResponse()).hasActualField(offerId).hasNoOutdated().hasRelevant(offerId);
    }

    @Test
    public void shouldAddToFavorites() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();

        FavoritesResponse favorites = api.favorites().patchFavoritesRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList(offerId))
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);

        assertThat(favorites.getResponse()).hasActualField(offerId).hasOutdated().hasRelevant(offerId);
    }

    @Test
    public void shouldRemoveFromFavorites() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();
        adaptor.addFavorites(token, offerId);

        FavoritesResponse favorites = api.favorites().patchFavoritesRoute().reqSpec(authSpec())
                .authorizationHeader(token).body(new FavoritesPatchRequest()
                        .remove(newArrayList(offerId))).execute(validatedWith(shouldBe200Ok()))
                .as(FavoritesResponse.class, GSON);

        assertThat(favorites.getResponse()).hasNoActualField().hasNoOutdated().hasNoRelevant();
    }
}
