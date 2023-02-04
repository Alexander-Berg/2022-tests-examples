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
import static ru.yandex.general.consts.Goals.PUBLIC_PROFILE_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «PUBLIC_PROFILE_SHOW» при показе публичного профиля")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class PublicProfileShowGoalTest {

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
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PUBLIC_PROFILE_SHOW)
    @DisplayName("Цель «PUBLIC_PROFILE_SHOW» при показе публичного профиля")
    public void shouldSeePublicProfileShowGoal() {
        goalsSteps.withGoalType(PUBLIC_PROFILE_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
