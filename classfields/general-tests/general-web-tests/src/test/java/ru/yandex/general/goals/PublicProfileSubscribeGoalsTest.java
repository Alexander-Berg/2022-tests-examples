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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.PUBLIC_PROFILE_SUBSCRIBE;
import static ru.yandex.general.consts.Goals.PUBLIC_PROFILE_UNSUBSCRIBE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.page.PublicProfilePage.LET;
import static ru.yandex.general.page.PublicProfilePage.SUBSCRIBE;
import static ru.yandex.general.page.PublicProfilePage.UNSUBSCRIBE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на странице публичного профиля")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class PublicProfileSubscribeGoalsTest {

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
        passportSteps.createAccountAndLogin();
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();

        basePageSteps.onProfilePage().sidebar().button(SUBSCRIBE).click();
        basePageSteps.onProfilePage().popup().button(LET).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PUBLIC_PROFILE_SUBSCRIBE)
    @DisplayName("Цель «PUBLIC_PROFILE_SUBSCRIBE» при подписке на профиль")
    public void shouldSeePublicProfileSubscribeGoal() {
        goalsSteps.withGoalType(PUBLIC_PROFILE_SUBSCRIBE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PUBLIC_PROFILE_UNSUBSCRIBE)
    @DisplayName("Цель «PUBLIC_PROFILE_UNSUBSCRIBE» при отписке от профиля")
    public void shouldSeePublicProfileUnsubscribeGoal() {
        basePageSteps.onProfilePage().sidebar().button(UNSUBSCRIBE).click();
        basePageSteps.onProfilePage().modal().button(UNSUBSCRIBE).waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(PUBLIC_PROFILE_UNSUBSCRIBE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
