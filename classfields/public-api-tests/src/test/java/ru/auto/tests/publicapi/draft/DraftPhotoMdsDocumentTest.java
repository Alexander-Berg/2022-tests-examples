package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /user/draft/{category}/{offerID}/photo/mds/document")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DraftPhotoMdsDocumentTest {

    private static final String PHOTO_PATH = "photo/photo.jpg";

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
    public void shouldAddDrivingLicense() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse =
                adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);

        String draftId = draftResponse.getOfferId();

        AutoApiStringListResponse response = api.photos().upload()
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        String uploadUrl = response.getValues().get(0);
        UploadResponse uploadResponse = adaptor.uploadImageFile(uploadUrl, PHOTO_PATH);

        AutoApiInternalMdsPhotoInfo body = new AutoApiInternalMdsPhotoInfo()
                .namespace(uploadResponse.getNamespace())
                .groupId(uploadResponse.getGroupId())
                .name(uploadResponse.getName());

        AutoApiPhotoSaveSuccessResponse photoResponse = api.draftPhotos().addDocumentPhotoDraft()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIDPath(draftId)
                .photoTypeQuery(AutoApiPhoto.PhotoTypeEnum.DRIVING_LICENSE)
                .body(body)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiDraftResponse draft = api.draft().getDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> photos = draft.getOffer().getState().getDocumentImage();

        AutoApiPhoto expectedPhoto = photoResponse.getPhoto();
        AutoApiPhoto testPhoto = photos.get(0);

        Assertions.assertThat(testPhoto.getName())
                .containsIgnoringCase(expectedPhoto.getName());
        Assertions.assertThat(testPhoto.getPhotoType())
                .isEqualByComparingTo(AutoApiPhoto.PhotoTypeEnum.DRIVING_LICENSE);
    }

    @Test
    public void shouldAddStsBack() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse =
                adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);

        String draftId = draftResponse.getOfferId();

        AutoApiStringListResponse response = api.photos().upload()
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        String uploadUrl = response.getValues().get(0);
        UploadResponse uploadResponse = adaptor.uploadImageFile(uploadUrl, PHOTO_PATH);

        AutoApiInternalMdsPhotoInfo body = new AutoApiInternalMdsPhotoInfo()
                .namespace(uploadResponse.getNamespace())
                .groupId(uploadResponse.getGroupId())
                .name(uploadResponse.getName());

        AutoApiPhotoSaveSuccessResponse photoResponse = api.draftPhotos().addDocumentPhotoDraft()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIDPath(draftId)
                .photoTypeQuery(AutoApiPhoto.PhotoTypeEnum.STS_BACK)
                .body(body)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiDraftResponse draft = api.draft().getDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> photos = draft.getOffer().getState().getDocumentImage();

        AutoApiPhoto expectedPhoto = photoResponse.getPhoto();
        AutoApiPhoto testPhoto = photos.get(0);

        Assertions.assertThat(testPhoto.getName())
                .containsIgnoringCase(expectedPhoto.getName());
        Assertions.assertThat(testPhoto.getPhotoType())
                .isEqualByComparingTo(AutoApiPhoto.PhotoTypeEnum.STS_BACK);
    }
}
