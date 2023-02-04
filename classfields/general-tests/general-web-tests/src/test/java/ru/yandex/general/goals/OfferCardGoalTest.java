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
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_COMPLAIN_CLICK;
import static ru.yandex.general.consts.Goals.CARD_OFFER_MAP_ROUTE_CLICK;
import static ru.yandex.general.consts.Goals.CARD_OFFER_MAP_SHOW;
import static ru.yandex.general.consts.Goals.CARD_OFFER_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.page.OfferCardPage.BUILD_A_ROUTE;
import static ru.yandex.general.page.OfferCardPage.COMPLAIN;
import static ru.yandex.general.page.OfferCardPage.SHOW_MAP;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
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
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().snippetFirst().click();
        basePageSteps.switchToNextTab();
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
        basePageSteps.onOfferCardPage().button(COMPLAIN).click();

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

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_MAP_ROUTE_CLICK)
    @DisplayName("Цель «CARD_OFFER_MAP_ROUTE_CLICK» при тапе на «Построить маршрут» на карте")
    public void shouldSeeCardOfferMapRouteClickGoal() {
        basePageSteps.onOfferCardPage().spanLink(SHOW_MAP).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onOfferCardPage().modal().button(BUILD_A_ROUTE).waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(CARD_OFFER_MAP_ROUTE_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
