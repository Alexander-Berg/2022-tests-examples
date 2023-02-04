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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.TABBAR_FAVOURITES_CLICK;
import static ru.yandex.general.consts.Goals.TABBAR_FORM_CLICK;
import static ru.yandex.general.consts.Goals.TABBAR_MAIN_CLICK;
import static ru.yandex.general.consts.Goals.TABBAR_PROFILE_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на таббар")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class TabbarGoalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.commonAccountLogin();
        urlSteps.testing();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(TABBAR_FAVOURITES_CLICK)
    @DisplayName("Цель «TABBAR_FAVOURITES_CLICK»")
    public void shouldSeeTabbarFavouritesClick() {
        urlSteps.open();
        basePageSteps.onBasePage().tabBar().favorites().click();

        goalsSteps.withGoalType(TABBAR_FAVOURITES_CLICK)
                .withPageRef(urlSteps.path(MOSKVA).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(TABBAR_PROFILE_CLICK)
    @DisplayName("Цель «TABBAR_PROFILE_CLICK»")
    public void shouldSeeTabbarProfileClick() {
        urlSteps.open();
        basePageSteps.onBasePage().tabBar().myOffers().click();

        goalsSteps.withGoalType(TABBAR_PROFILE_CLICK)
                .withPageRef(urlSteps.path(MOSKVA).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(TABBAR_MAIN_CLICK)
    @DisplayName("Цель «TABBAR_MAIN_CLICK»")
    public void shouldSeeTabbarMainClick() {
        urlSteps.path(MY).path(OFFERS).open();
        basePageSteps.onBasePage().tabBar().mainPage().click();

        goalsSteps.withGoalType(TABBAR_MAIN_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(TABBAR_FORM_CLICK)
    @DisplayName("Цель «TABBAR_FORM_CLICK»")
    public void shouldSeeTabbarFormClick() {
        urlSteps.open();
        basePageSteps.onBasePage().tabBar().addOffer().click();

        goalsSteps.withGoalType(TABBAR_FORM_CLICK)
                .withPageRef(urlSteps.path(MOSKVA).toString())
                .withCount(1)
                .shouldExist();
    }

}
