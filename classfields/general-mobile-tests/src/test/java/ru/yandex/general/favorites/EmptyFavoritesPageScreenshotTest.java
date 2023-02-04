package ru.yandex.general.favorites;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.LIGHT_THEME;

@Epic(FAVORITES_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотный тест пустой страницы избранного")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EmptyFavoritesPageScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

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
        passportSteps.commonAccountLogin();
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот пустой страницы избранного. Раздел «Объявления», светлая/темная темы")
    public void shouldSeeEmptyFavoritesScreenshot() {
        urlSteps.testing().path(MY).path(FAVORITES).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот пустой страницы избранного. Раздел «Поиски», светлая/темная темы")
    public void shouldSeeEmptyFavoritesSearchesScreenshot() {
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот пустой страницы избранного. Раздел «Профили», светлая/темная темы")
    public void shouldSeeEmptyFavoritesSellerScreenshot() {
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
