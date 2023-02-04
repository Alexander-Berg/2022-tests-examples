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
import static ru.yandex.general.consts.Goals.HEADER_CHAT_CLICK;
import static ru.yandex.general.consts.Goals.HEADER_PROFILE_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на хэдер")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class HeaderGoalTest {

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
    @Feature(HEADER_PROFILE_CLICK)
    @DisplayName("Цель «HEADER_PROFILE_CLICK»")
    public void shouldSeeHeaderProfileClick() {
        urlSteps.open();
        basePageSteps.onBasePage().header().burger().click();

        goalsSteps.withGoalType(HEADER_PROFILE_CLICK)
                .withPageRef(urlSteps.path(MOSKVA).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(HEADER_CHAT_CLICK)
    @DisplayName("Цель «HEADER_CHAT_CLICK»")
    public void shouldSeeHeaderChatClick() {
        urlSteps.open();
        basePageSteps.onBasePage().header().chats().click();

        goalsSteps.withGoalType(HEADER_CHAT_CLICK)
                .withPageRef(urlSteps.path(MOSKVA).toString())
                .withCount(1)
                .shouldExist();
    }

}
