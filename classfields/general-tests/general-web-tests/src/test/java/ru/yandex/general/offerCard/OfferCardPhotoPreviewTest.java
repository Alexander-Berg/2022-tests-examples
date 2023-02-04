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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PREVIEW_GALLERY;
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
@Feature(PREVIEW_GALLERY)
@DisplayName("Тесты на превью фото на карточке")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardPhotoPreviewTest {

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
    @DisplayName("Нет списка превью фото на карточке с одним фото")
    public void shouldNotSeePreviewListWithOnePhoto() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().previewsList().should(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("1 превью фото + 1 превью видео, на карточке с одним фото и видео")
    public void shouldSeeTwoPreviewWithOnePhotoAndVideo() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(1).setVideoUrl(VIDEO_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().previewsList().should(hasSize(1));
        basePageSteps.onOfferCardPage().videoPreview().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("2 превью фото на карточке с двумя фото")
    public void shouldSeePreviewListWithTwoPhoto() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(2).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().previewsList().should(hasSize(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Ещё n фото» с 5 фото")
    public void shouldNotSeeMorePhotoButtonWithFivePhoto() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(5).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().morePhoto().should(not(isDisplayed()));
        basePageSteps.onOfferCardPage().previewsList().should(hasSize(5));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Главным фото отображается первая фото из списка")
    public void shouldSeeMainPhotoFirstFromPhotoList() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().mainPhoto().should(hasAttribute(SRC, containsString(FIRST_PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Главным фото сменяется по клику на фото из превью")
    public void shouldChangeMainPhotoByPreviewClick() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().previewsList().get(1).click();

        basePageSteps.onOfferCardPage().mainPhoto().should(hasAttribute(SRC, containsString(SECOND_PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Главным фото сменяется по клику на видео из превью")
    public void shouldChangeMainPhotoByVideoClick() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(2).setVideoUrl(VIDEO_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().videoPreview().click();

        basePageSteps.onOfferCardPage().mainVideo().should(isDisplayed());
    }

}
