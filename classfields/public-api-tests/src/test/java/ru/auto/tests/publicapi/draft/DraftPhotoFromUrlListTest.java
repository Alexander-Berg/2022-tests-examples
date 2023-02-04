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

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /user/draft/{category}/{offerID}/photo/from-url-list")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DraftPhotoFromUrlListTest {

    private static final String PHOTO_URL1 =
            "https://avatars.mds.yandex.net/get-verba/1030388/2a000001609d6b9dadd66d7d9aef28b9cec4/1200x900n";
    private static final String PHOTO_URL2 =
            "https://avatars.mds.yandex.net/get-verba/787013/2a000001609cf207659cee6ea795c8ba6c2c/1200x900n";

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
    public void shouldAddPhotoFromUrlList() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiDraftResponse draftResponse =
                adaptor.createDraft(account.getLogin(), sessionId, AutoApiOffer.CategoryEnum.CARS);

        String draftId = draftResponse.getOfferId();
        String body = PHOTO_URL1 + "\n" + PHOTO_URL2;

        api.draftPhotos().createPhotoFromUrlList()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIDPath(draftId)
                .body(body)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiDraftResponse draft = api.draft().getDraft().categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIdPath(draftId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        List<AutoApiPhoto> photos = draft.getOffer().getState().getImageUrls();

        Assertions.assertThat(photos).hasSize(2);
    }
}
