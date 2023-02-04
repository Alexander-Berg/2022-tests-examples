package ru.auto.tests.realtyapi.v1.user.offer;


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
import ru.auto.tests.realtyapi.responses.GetUserOffersByIdResp;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.ExampleImagesModel;
import ru.auto.tests.realtyapi.v1.model.ExampleImagesModelFields;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404OfferNotFound;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404UnknownVosUser;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;


@Title("PUT /user/offers/{offerId}/update_images")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class UpdateImagesTest {

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
        api.userOffers().updateImagesRoute().offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.userOffers().updateImagesRoute().offerIdPath(getRandomOfferId()).reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.userOffers().updateImagesRoute().offerIdPath(getRandomOfferId())
                .reqSpec(authSpec()).authorizationHeader(token)
                .body(random(ExampleImagesModel.class))
                .execute(validatedWith(shouldBe404UnknownVosUser(account.getId())));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = getRandomOfferId();
        adaptor.vosUser(token);
        api.userOffers().updateImagesRoute().offerIdPath(offerId).reqSpec(authSpec()).authorizationHeader(token)
                .body(random(ExampleImagesModel.class))
                .execute(validatedWith(shouldBe404OfferNotFound(offerId)));
    }

    @Test
    public void shouldUpdateImages() {
        Account account = am.create();
        String imageUrl = "//avatars.mdst.yandex.net/get-realty/3022/add.1580733118413bc98a80118";
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();
        ExampleImagesModel reqBody = random(ExampleImagesModel.class);

        reqBody.getImages().clear();
        reqBody.getImages().add(random(ExampleImagesModelFields.class).url(format("http:%s", imageUrl)).active(true));

        api.userOffers().getUserOfferRoute().reqSpec(authSpec()).authorizationHeader(token)
                .offerIdPath(offerId).execute(validatedWith(shouldBeCode(SC_OK)));
        api.userOffers().updateImagesRoute().offerIdPath(offerId).reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody).execute(validatedWith(shouldBeCode(SC_OK)));

        GetUserOffersByIdResp resp = api.userOffers().getUserOfferRoute().reqSpec(authSpec()).authorizationHeader(token)
                .offerIdPath(offerId).execute(validatedWith(shouldBeCode(SC_OK)))
                .as(GetUserOffersByIdResp.class, GSON);

        assertThat(resp.getResponse())
                .hasAppMiniSnippetImages(format("%s/app_snippet_mini", imageUrl))
                .hasAlikeImages(format("%s/alike", imageUrl))
                .hasCosmicImages(format("%s/cosmic", imageUrl))
                .hasAppMiddleSnippetImages(format("%s/app_snippet_middle", imageUrl))
                .hasAppLargeSnippetImages(format("%s/app_snippet_large", imageUrl))
                .hasFullImages(format("%s/large", imageUrl))
                .hasAppMiddleImages(format("%s/app_middle", imageUrl))
                .hasAppLargeImages(format("%s/app_large", imageUrl))
                .hasMinicardImages(format("%s/minicard", imageUrl))
                .hasMainImages(format("%s/main", imageUrl))
                .hasAppSmallSnippetImages(format("%s/app_snippet_small", imageUrl));
    }
}
