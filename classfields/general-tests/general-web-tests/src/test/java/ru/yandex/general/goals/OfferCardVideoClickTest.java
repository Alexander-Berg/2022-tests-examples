package ru.yandex.general.goals;

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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_VIDEO_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «CARD_OFFER_VIDEO_CLICK», при воспроизведении видео на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class OfferCardVideoClickTest {

    private static final String ID = "123456";
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=k04YxBCWHdc";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setId(ID).setVideoUrl(VIDEO_URL).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().path(CARD).path(ID).open();
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_VIDEO_CLICK)
    @DisplayName("Цель «CARD_OFFER_VIDEO_CLICK», при воспроизведении видео на карточке оффера")
    public void shouldSeeCardOfferVideoClick() {
        basePageSteps.onOfferCardPage().videoPreview().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().video().click();

        goalsSteps.withGoalType(CARD_OFFER_VIDEO_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_VIDEO_CLICK)
    @DisplayName("Цель «CARD_OFFER_VIDEO_CLICK», при воспроизведении видео в фуллскрин галерее на карточке оффера")
    public void shouldSeeCardOfferVideoClickFullscreenGallery() {
        basePageSteps.onOfferCardPage().mainPhoto().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().videoPreview().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().fullscreenGallery().video().click();

        goalsSteps.withGoalType(CARD_OFFER_VIDEO_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
