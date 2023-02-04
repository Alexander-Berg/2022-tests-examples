package ru.yandex.general.collorScheme;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.COLOR_SCHEME;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;

@Epic(COLOR_SCHEME)
@Feature("Отображение цветовой схемы")
@DisplayName("Отображение страниц ЛК с соответствующей цветовой схемой")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CollorSchemeViewLKTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String theme;

    @Parameterized.Parameters(name = "{index}. Тема «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {LIGHT_THEME},
                {DARK_THEME}
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются «Мои объявления» в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnMyOffers() {
        urlSteps.path(OFFERS).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются «Избранные» в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnMyFavorites() {
        urlSteps.path(FAVORITES).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются «Настройки профиля» в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnMyContacts() {
        urlSteps.path(CONTACTS).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается «Автозагрузка» в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnMyFeed() {
        urlSteps.path(FEED).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается «Статистика» в соответствующей цветовой схеме")
    public void shouldSeeColorThemeOnMyStatistics() {
        urlSteps.path(STATS).open();

        basePageSteps.onListingPage().pageColor(theme).should(isDisplayed());
    }

}
