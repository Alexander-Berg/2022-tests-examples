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
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiPhoto;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;
import java.util.stream.Collectors;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("PUT /user/draft/{category}/{offerID}/photo/{photoID}/rotate")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DraftPhotoRotateTest {

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
    public void shouldRotateCw() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse =
                adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);

        String draftId = draftResponse.getOfferId();
        AutoApiPhoto addedPhoto =
                adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), PHOTO_PATH);
        addedPhoto.getSizes().remove("orig");

        AutoApiPhoto rotated90 = adaptor.rotateDraftImageCw(sessionId, draftId, addedPhoto.getName());
        rotated90.getSizes().remove("orig");
        check(sessionId, draftId, rotated90, 90);

        AutoApiPhoto rotated180 = adaptor.rotateDraftImageCw(sessionId, draftId, rotated90.getName());
        rotated180.getSizes().remove("orig");
        check(sessionId, draftId, rotated180, 180);

        AutoApiPhoto rotated270 = adaptor.rotateDraftImageCw(sessionId, draftId, rotated180.getName());
        rotated270.getSizes().remove("orig");
        check(sessionId, draftId, rotated270, 270);

        AutoApiPhoto rotated0 = adaptor.rotateDraftImageCw(sessionId, draftId, rotated270.getName());
        rotated0.getSizes().remove("orig");
        check(sessionId, draftId, rotated0, 0);
    }

    @Test
    public void shouldRotateCcw() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse =
                adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);

        String draftId = draftResponse.getOfferId();
        AutoApiPhoto addedPhoto =
                adaptor.uploadImageFileToDraft(draftResponse.getOffer().getState().getUploadUrl(), PHOTO_PATH);

        AutoApiPhoto rotated270 = adaptor.rotateDraftImageCcw(sessionId, draftId, addedPhoto.getName());
        rotated270.getSizes().remove("orig");
        check(sessionId, draftId, rotated270, 270);

        AutoApiPhoto rotated180 = adaptor.rotateDraftImageCcw(sessionId, draftId, rotated270.getName());
        rotated180.getSizes().remove("orig");
        check(sessionId, draftId, rotated180, 180);

        AutoApiPhoto rotated90 = adaptor.rotateDraftImageCcw(sessionId, draftId, rotated180.getName());
        rotated90.getSizes().remove("orig");
        check(sessionId, draftId, rotated90, 90);

        AutoApiPhoto rotated0 = adaptor.rotateDraftImageCcw(sessionId, draftId, rotated90.getName());
        rotated0.getSizes().remove("orig");
        check(sessionId, draftId, rotated0, 0);
    }

    private void check(String sessionId, String draftId, AutoApiPhoto expectedPhoto, Integer expectedAngle) {
        AutoApiDraftResponse draft = api.draft().getDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> photos = draft.getOffer().getState().getImageUrls().stream().map(r ->
                r.createDate(null)).collect(Collectors.toList());

        Assertions.assertThat(photos).hasSize(1).containsOnlyOnce(expectedPhoto);
        AutoApiPhoto photo = photos.get(0);
        Assertions.assertThat(photo.getTransform().getAngle()).isEqualTo(expectedAngle);
    }
}
