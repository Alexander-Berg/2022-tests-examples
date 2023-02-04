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
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FULLSCREEN_GALLERY;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.FullscreenGallery.ZOOMED_IN;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(FULLSCREEN_GALLERY)
@DisplayName("Тесты на зум фуллскрин галереи")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardFullscreenGalleryZoomTest {

    private static final String ID = "12345";
    private static final String SECOND_PHOTO = "https://avatars.mdst.yandex.net/get-o-yandex/1398656/2f07dbf88c5490a0e4e9f5f41ba2d10e/";
    private MockResponse mockResponse;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

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
    @DisplayName("Зумим фото в фуллскрин галерее по левому клику, у оффера одно фото")
    public void shouldSeeZoomedFullscreenPhotoOneImage() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(hasClass(containsString(ZOOMED_IN)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Зумим фото в фуллскрин галерее по левому клику, у оффера два фото")
    public void shouldSeeZoomedFullscreenPhotoTwoImage() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(2).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(hasClass(containsString(ZOOMED_IN)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет зума фото в фуллскрин галерее по правому клику, у оффера одно фото")
    public void shouldSeeNoZoomedFullscreenPhotoByContextClickOneImage() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.contextClick(basePageSteps.onOfferCardPage().fullscreenGallery());

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(hasClass(containsString(ZOOMED_IN))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет зума фото в фуллскрин галерее по правому клику, у оффера два фото")
    public void shouldSeeNoZoomedFullscreenPhotoByContextClickTwoImage() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(2).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.contextClick(basePageSteps.onOfferCardPage().fullscreenGallery());

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(hasClass(containsString(ZOOMED_IN))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отменяем зум фото в фуллскрин галерее по левому клику, у оффера одно фото")
    public void shouldSeeCancelZoomFullscreenPhotoOneImage() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(hasClass(containsString(ZOOMED_IN))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отменяем зум фото в фуллскрин галерее по левому клику, у оффера два фото")
    public void shouldSeeCancelZoomFullscreenPhotoTwoImage() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(2).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(hasClass(containsString(ZOOMED_IN))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("При переключении между превью фото в фуллскрин галерее сбрасывается зум")
    public void shouldSeeCancelZoomAfterChangePhoto() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().get(1).click();
        basePageSteps.onOfferCardPage().fullscreenGallery().galleryItemList().get(2).waitUntil(
                hasAttribute(SRC, containsString(SECOND_PHOTO)));

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(hasClass(containsString(ZOOMED_IN))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Зумим, переключаемся на другое фото в фуллскрин галерее, возвращаемся на первое - зума нет")
    public void shouldSeeCancelZoomAfterReturnToPhoto() {
        mockRule.graphqlStub(mockResponse.setCard(
                        mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().get(1).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().previewsList().get(0).click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(hasClass(containsString(ZOOMED_IN))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фуллскрин галерея с фото в зуме закрывается по крестику")
    public void shouldCloseFullscreenGallery() {
        mockRule.graphqlStub(mockResponse.setCard(mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();
        openFullscreenGallery();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().close().click();

        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(isDisplayed()));
    }

    private void openFullscreenGallery() {
        basePageSteps.onOfferCardPage().mainPhoto().click();
        basePageSteps.onOfferCardPage().fullscreenGallery().waitUntil(isDisplayed());
    }

}
