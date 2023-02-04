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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
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
@GuiceModules(GeneralMobileWebModule.class)
public class OfferCardFullscreenGalleryTest {

    private static final String ID = "12345";
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
    @DisplayName("Фуллскрин галерея закрывается по крестику")
    public void shouldCloseFullscreenGallery() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().photoPreviewList().get(0).click();
        basePageSteps.onOfferCardPage().fullscreenGallery().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().fullscreenGallery().close().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открывается первое фото в фуллскрин галерее по клику на первое фото на карточке")
    public void shouldSeeFirstFullscreenPreview() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().photoPreviewList().get(0).click();

        basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(1).image().should(
                hasAttribute(SRC, containsString(FIRST_PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открывается второе фото в фуллскрин галерее по свайпу")
    public void shouldSeeSecondFullscreenPhotoAfterSwipe() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().photoPreviewList().get(0).click();
        basePageSteps.moveSlider(basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(1).image(),
                basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(2).image());

        basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(2).image().should(
                hasAttribute(SRC, containsString(SECOND_PHOTO)));
    }

}
