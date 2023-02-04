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
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PREVIEW_GALLERY;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mobile.element.Image.SRC;
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
@GuiceModules(GeneralMobileWebModule.class)
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
    @DisplayName("1 превью фото, на карточке с одним фото")
    public void shouldSeeOnePreviewWithOnePhoto() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(1).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().photoPreviewList().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("1 превью фото + 1 превью видео, на карточке с одним фото и видео")
    public void shouldSeeTwoPreviewWithOnePhotoAndVideo() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(1).setVideoUrl(VIDEO_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().photoPreviewList().should(hasSize(1));
        basePageSteps.onOfferCardPage().video().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("20 превью фото + 1 превью видео, на карточке с одним фото и видео")
    public void shouldSeeMaxMediaPreview() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(20).setVideoUrl(VIDEO_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().photoPreviewList().should(hasSize(20));
        basePageSteps.onOfferCardPage().video().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Превью отображаются в нужном порядке")
    public void shouldSeePreviewPhotoInOrder() {
        mockRule.graphqlStub(mockResponse.setCard(
                mockCard(BASIC_CARD).addPhoto(PHOTO_1, PHOTO_2, PHOTO_3).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().photoPreviewList().get(0).image().should(
                hasAttribute(SRC, containsString(FIRST_PHOTO)));
        basePageSteps.onOfferCardPage().photoPreviewList().get(1).image().should(
                hasAttribute(SRC, containsString(SECOND_PHOTO)));
    }

}
