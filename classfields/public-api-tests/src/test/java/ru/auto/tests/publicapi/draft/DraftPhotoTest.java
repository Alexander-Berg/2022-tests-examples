package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplate;
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiPhoto;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("ADD/DELETE/UPDATE draft photo")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
@Issue("AUTORUAPI-2667")
public class DraftPhotoTest {

    private static final String CARS_OFFER = "offers/cars.ftl";
    private static final String PHOTO_PATH = "photo/photo.jpg";
    private static final String SECOND_PHOTO_PATH = "photo/photo1.jpg";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldDeletePhoto() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse = adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);
        AutoApiPhoto expectedPhoto = adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), PHOTO_PATH);

        expectedPhoto.getSizes().remove("orig");

        adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), SECOND_PHOTO_PATH);

        AutoApiOffer offer = getOfferRequest(account.getLogin());
        offer.getState().setImageUrls(newArrayList(expectedPhoto));

        AutoApiDraftResponse response = api.draft().updateDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftResponse.getOfferId())
                .body(offer)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));


        List<AutoApiPhoto> cleanResult = (response.getOffer().getState().getImageUrls().stream().map(r ->
                r.createDate(null)).collect(Collectors.toList()));
        Assertions.assertThat(cleanResult).hasSize(1).containsOnlyOnce(expectedPhoto);
    }

    @Test
    @Ignore
    public void shouldAddTwoSamePhotos() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse = adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);
        AutoApiPhoto expectedPhoto = adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), PHOTO_PATH);

        expectedPhoto.getSizes().remove("orig");

        AutoApiOffer offer = getOfferRequest(account.getLogin());
        offer.getState().setImageUrls(newArrayList(expectedPhoto, expectedPhoto));

        AutoApiDraftResponse response = api.draft().updateDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftResponse.getOfferId())
                .body(offer)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));
        List<AutoApiPhoto> cleanResult = (response.getOffer().getState().getImageUrls().stream().map(r ->
                r.createDate(null)).collect(Collectors.toList()));
        Assertions.assertThat(cleanResult).hasSize(1).containsOnly(expectedPhoto);
    }

    @Test
    public void shouldAddPhotoViaUploader() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse = adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);
        AutoApiPhoto expectedPhoto = adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), PHOTO_PATH);

        expectedPhoto.getSizes().remove("orig");

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftResponse.getOfferId())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> cleanResult = (response.getOffer().getState().getImageUrls().stream().map(r ->
                r.createDate(null)).collect(Collectors.toList()));
        Assertions.assertThat(cleanResult).hasSize(1).containsOnlyOnce(expectedPhoto);
    }

    @Test
    public void shouldAddTwoPhotosViaUploader() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse = adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);
        AutoApiPhoto expectedPhoto = adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), PHOTO_PATH);

        expectedPhoto.getSizes().remove("orig");

        AutoApiPhoto secondExpectedPhoto = adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), SECOND_PHOTO_PATH);
        secondExpectedPhoto.getSizes().remove("orig");

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftResponse.getOfferId())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> cleanResult = (response.getOffer().getState().getImageUrls().stream().map(r ->
                r.createDate(null)).collect(Collectors.toList()));

        Assertions.assertThat(cleanResult).hasSize(2).containsOnlyOnce(expectedPhoto, secondExpectedPhoto);
    }

    @Test
    public void shouldDeleteByPhotoId() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse =
                adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);
        AutoApiPhoto expectedPhoto =
                adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), PHOTO_PATH);

        expectedPhoto.getSizes().remove("orig");

        AutoApiPhoto secondExpectedPhoto =
                adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), SECOND_PHOTO_PATH);
        secondExpectedPhoto.getSizes().remove("orig");

        AutoApiDraftResponse draftWithTwoPhotoResponse = api.draft().getDraft()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftResponse.getOfferId())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> addedPhotos =
                (draftWithTwoPhotoResponse.getOffer().getState().getImageUrls().stream().map(r ->
                        r.createDate(null)).collect(Collectors.toList()));

        Assertions.assertThat(addedPhotos).hasSize(2).containsOnlyOnce(expectedPhoto, secondExpectedPhoto);

        api.draftPhotos().deletePhoto()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIDPath(draftResponse.getOfferId())
                .photoIDPath(expectedPhoto.getName())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiDraftResponse draftWithOnePhotoResponse = api.draft().getDraft()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftResponse.getOfferId())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> afterPhotos =
                (draftWithOnePhotoResponse.getOffer().getState().getImageUrls().stream().map(r ->
                        r.createDate(null)).collect(Collectors.toList()));

        Assertions.assertThat(afterPhotos).hasSize(1).containsOnlyOnce(secondExpectedPhoto);
    }

    private AutoApiOffer getOfferRequest(String login) {
        String body = new OfferTemplate().process(CARS_OFFER, login);
        return new GsonBuilder().create().fromJson(body, AutoApiOffer.class);
    }
}
