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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_COMPLAIN_CLICK;
import static ru.yandex.general.consts.Goals.CARD_OFFER_MAP_SHOW;
import static ru.yandex.general.consts.Goals.CARD_OFFER_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mobile.page.OfferCardPage.COMPLAIN;
import static ru.yandex.general.mobile.page.OfferCardPage.SHOW_MAP;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class OfferCardGoalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().snippetFirst().hover().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_SHOW)
    @DisplayName("Цель «CARD_OFFER_SHOW» при открытии карточки оффера")
    public void shouldSeeCardOfferShowGoalOnOpenCard() {
        goalsSteps.withGoalType(CARD_OFFER_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_COMPLAIN_CLICK)
    @DisplayName("Цель «CARD_OFFER_COMPLAIN_CLICK» при нажатии на «Пожаловаться» на карточке")
    public void shouldSeeCardOfferComplainClickGoal() {
        basePageSteps.resize(375, 2000);
        basePageSteps.onOfferCardPage().spanLink(COMPLAIN).click();

        goalsSteps.withGoalType(CARD_OFFER_COMPLAIN_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_MAP_SHOW)
    @DisplayName("Цель «CARD_OFFER_MAP_SHOW» при отображении карты")
    public void shouldSeeCardOfferMapShowGoal() {
        basePageSteps.onOfferCardPage().spanLink(SHOW_MAP).click();

        goalsSteps.withGoalType(CARD_OFFER_MAP_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
