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

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.COLOR_SCHEME_DARK;
import static ru.yandex.general.consts.Goals.COLOR_SCHEME_LIGHT;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.page.ContactsPage.DARK;
import static ru.yandex.general.page.ContactsPage.LIGHT;

@Epic(GOALS_FEATURE)
@DisplayName("Цели при смене цветовой схемы")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ColorSchemeGoalsTest {

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
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(CONTACTS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(COLOR_SCHEME_DARK)
    @DisplayName("Цель «COLOR_SCHEME_DARK», при установке тёмной темы")
    public void shouldSeeColorSchemeDark() {
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(DARK).click();

        goalsSteps.withGoalType(COLOR_SCHEME_DARK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(COLOR_SCHEME_LIGHT)
    @DisplayName("Цель «COLOR_SCHEME_LIGHT», при установке светлой темы")
    public void shouldSeeColorSchemeLight() {
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(DARK).click();
        basePageSteps.refresh();
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        goalsSteps.clearHar();
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(LIGHT).click();

        goalsSteps.withGoalType(COLOR_SCHEME_LIGHT)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
