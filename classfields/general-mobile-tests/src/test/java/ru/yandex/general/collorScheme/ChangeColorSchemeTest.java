package ru.yandex.general.collorScheme;

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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.COLOR_SCHEME;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mobile.page.ContactsPage.DARK;
import static ru.yandex.general.mobile.page.ContactsPage.LIGHT;
import static ru.yandex.general.mobile.page.ContactsPage.SYSTEM;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.LIGHT_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.SYSTEM_THEME;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;

@Epic(COLOR_SCHEME)
@Feature("Изменение цветовой схемы")
@DisplayName("Цветовая схема")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class ChangeColorSchemeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(CONTACTS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение дефолтной светлой темы")
    public void shouldSeeDefaultColorLight() {
        urlSteps.open();

        basePageSteps.onContactsPage().pageColor(LIGHT_THEME).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение тёмной темы при смене с светлой")
    public void shouldSeeChangeLightToDark() {
        urlSteps.open();
        basePageSteps.onContactsPage().pageColor(LIGHT_THEME).waitUntil(isDisplayed());
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(DARK).click();

        basePageSteps.onContactsPage().pageColor(DARK_THEME).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение системной светлой темы")
    public void shouldSeeChangeLightToSystem() {
        urlSteps.open();
        basePageSteps.onContactsPage().pageColor(LIGHT_THEME).waitUntil(isDisplayed());
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(SYSTEM).click();

        basePageSteps.onContactsPage().pageColor(LIGHT_THEME).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение светлой темы при смене с тёмной")
    public void shouldSeeChangeDarkToLight() {
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, BasePageSteps.DARK_THEME);
        urlSteps.open();
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(LIGHT).click();

        basePageSteps.onContactsPage().pageColor(LIGHT_THEME).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена темы на тёмную, установка куки")
    public void shouldSeeSetCookieDarkTheme() {
        urlSteps.open();
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(DARK).click();

        basePageSteps.shouldSeeCookie(CLASSIFIED_USER_THEME, DARK_THEME);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена темы на системную, установка куки")
    public void shouldSeeSetCookieSystemTheme() {
        urlSteps.open();
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(SYSTEM).click();

        basePageSteps.shouldSeeCookie(CLASSIFIED_USER_THEME, SYSTEM_THEME);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена темы на светлую, установка куки")
    public void shouldSeeSetCookieLightTheme() {
        urlSteps.open();
        basePageSteps.onContactsPage().colorSchemeSelector().click();
        basePageSteps.onContactsPage().popup().menuItem(LIGHT).click();

        basePageSteps.shouldSeeCookie(CLASSIFIED_USER_THEME, LIGHT_THEME);
    }

}
