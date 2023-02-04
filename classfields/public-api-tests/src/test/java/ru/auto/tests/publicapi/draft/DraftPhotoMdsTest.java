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
import java.util.stream.Collectors;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /user/draft/{category}/{offerID}/photo/mds")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DraftPhotoMdsTest {

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
    public void shouldAddPhoto() {
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

        AutoApiPhotoSaveSuccessResponse photoResponse = api.draftPhotos().addPhoto()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIDPath(draftId)
                .body(body)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiPhoto expectedPhoto = photoResponse.getPhoto();
        expectedPhoto.getSizes().remove("orig");

        AutoApiDraftResponse draft = api.draft().getDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> photos = draft.getOffer().getState().getImageUrls().stream().map(r ->
                r.createDate(null)).collect(Collectors.toList());

        Assertions.assertThat(photos).hasSize(1).containsOnlyOnce(expectedPhoto);
    }
}
