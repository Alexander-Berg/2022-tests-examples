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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(FAVORITES_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотный тест страницы избранного на разных разрешениях экрана")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesScreenshotTest {

    private static final String IGNORE_DUMMY_IMG = "//div[contains(@class, '_emptyPhoto_')]";

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

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public int width;

    @Parameterized.Parameter(1)
    public String theme;

    @Parameterized.Parameters(name = "Скриншот избранного на разных разрешениях. Тема «{1}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {1920, LIGHT_THEME},
                {1920, DARK_THEME},
                {1366, LIGHT_THEME},
                {1366, DARK_THEME}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setFavoritesExample()
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        compareSteps.resize(width, 1500);
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        basePageSteps.setMoscowCookie();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы избранного. Раздел «Объявления» на разных разрешениях экрана, светлая/темная темы")
    public void shouldSeeFavoritesScreenshot() {
        urlSteps.testing().path(MY).path(FAVORITES).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы избранного. Раздел «Поиски» на разных разрешениях экрана, светлая/темная темы")
    public void shouldSeeFavoritesSearchesScreenshot() {
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
        Screenshot testing = compareSteps.getElementScreenshotIgnoreElements(
                basePageSteps.onFavoritesPage().pageRoot(),
                newHashSet(IGNORE_DUMMY_IMG));

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshotIgnoreElements(
                basePageSteps.onFavoritesPage().pageRoot(),
                newHashSet(IGNORE_DUMMY_IMG));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы избранного. Раздел «Профили» на разных разрешениях экрана, светлая/темная темы")
    public void shouldSeeFavoritesSellersScreenshot() {
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот прилипшего хэдера на странице избранного на разных разрешениях экрана, светлая/темная темы")
    public void shouldSeeFloatedHeaderFavoritesScreenshot() {
        urlSteps.testing().path(MY).path(FAVORITES).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        urlSteps.setProductionHost().open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
