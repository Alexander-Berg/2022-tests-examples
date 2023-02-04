package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FULLSCREEN_GALLERY;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.Image.SRC;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.PHOTO_1;
import static ru.yandex.general.mock.MockCard.PHOTO_2;
import static ru.yandex.general.mock.MockCard.PHOTO_3;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(FULLSCREEN_GALLERY)
@DisplayName("Тесты на фуллскрин галерею")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardFullscreenGalleryTest {

    private static final String ID = "12345";
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=k04YxBCWHdc";
    private static final String FIRST_PHOTO = "https://avatars.mdst.yandex.net/get-o-yandex/65675/af6807fe8f1796887c7e6907389a38f9/";
    private static final String SECOND_PHOTO = "https://avatars.mdst.yandex.net/get-o-yandex/1398656/2f07dbf88c5490a0e4e9f5f41ba2d10e/";
    private MockResponse mockResponse;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        urlSteps.testing().path(CARD).path(ID);
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        mockResponse = mockResponse().setCategoriesTemplate().setRegionsTemplate();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет списка превью фото на фуллскрин галерее с одним фото")
    public void shouldNotSeePreviewListFullscreenGalleryWithOnePhoto() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();

        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().should(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("2 превью на фуллскрин галерее с одним фото и видео")
    public void shouldSeeTwoPreviewFullscreenGalleryWithOnePhotoAndVideo() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(1).setVideoUrl(VIDEO_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();

        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().should(hasSize(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("2 превью на фуллскрин галерее с двумя фото")
    public void shouldSeeTwoPreviewFullscreenGalleryWithTwoPhoto() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(2).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();

        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().should(hasSize(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("21 превью на фуллскрин галерее с двадцатью фото и видео")
    public void shouldSeeTwentyOnePreviewFullscreenGalleryWithTwentyPhotoAndVideo() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(20).setVideoUrl(VIDEO_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();

        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().should(hasSize(21));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фуллскрин галерея закрывается по крестику")
    public void shouldCloseFullscreenGallery() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.onOfferCardPage().fullscreenGallery().close().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открывается первое фото в фуллскрин галерее по клику на главное фото оффера")
    public void shouldSeeFirstFullscreenPreview() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();

        basePageSteps.onOfferCardPage().fullscreenGallery().activePreview().should(
                hasAttribute(SRC, containsString(FIRST_PHOTO)));
        basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(0).should(
                hasAttribute(SRC, containsString(FIRST_PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открывается второе фото в фуллскрин галерее по клику на фото из превью карточки")
    public void shouldSeeFullscreenPhotoFromCardPreview() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().previewsList().get(1).click();
        basePageSteps.onOfferCardPage().mainPhoto().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().activePreview().should(
                hasAttribute(SRC, containsString(SECOND_PHOTO)));
        basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(1).should(
                hasAttribute(SRC, containsString(SECOND_PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена фото в фуллскрин галерее")
    public void shouldSeeChangedPhotoFromFullscreenPreview() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().get(1).click();

        basePageSteps.onOfferCardPage().fullscreenGallery().activePreview().should(
                hasAttribute(SRC, containsString(SECOND_PHOTO)));
        basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(2).should(
                hasAttribute(SRC, containsString(SECOND_PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот фуллскрин галереи с 5 фото и видео")
    public void shouldSeeFullscreenGalleryScreenshot() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(5).setVideoUrl(VIDEO_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMyOffersPage().pageRoot());

        urlSteps.setProductionHost().open();
        openFullscreenGallery();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMyOffersPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private void openFullscreenGallery() {
        basePageSteps.onOfferCardPage().mainPhoto().click();
        basePageSteps.onOfferCardPage().fullscreenGallery().waitUntil(isDisplayed());
    }

}
