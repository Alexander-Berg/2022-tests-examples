package ru.yandex.general.suggest;

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
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(GEO_SUGGEST_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты формы подачи оффера")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SuggestScreenshotTest {

    private static final String MITINO = "Митино";
    private static final String SANKT_PETERBURG = "Санкт-Петербург";
    private static final String CATEGORY = "Кеды";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

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
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        basePageSteps.setMoscowCookie();
        compareSteps.resize(1920, 1080);
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста категорий, светлая/темная темы")
    public void shouldSeeCategoryScreenshot() {
        basePageSteps.onListingPage().searchBar().input().sendKeys(CATEGORY);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(CATEGORY);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот карты, светлая/темная темы")
    public void shouldSeeMapScreenshot() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста на карте, светлая/темная темы")
    public void shouldSeeMapSuggestScreenshot() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(MITINO);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(MITINO);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот схемы метро, светлая/темная темы")
    public void shouldSeeSubwayScreenshot() {
        openSubwayMap();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        openSubwayMap();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста метро, светлая/темная темы")
    public void shouldSeeSubwaySuggestScreenshot() {
        openSubwayMap();
        basePageSteps.onListingPage().searchBar().fillSearchInput(MITINO);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        openSubwayMap();
        basePageSteps.onListingPage().searchBar().fillSearchInput(MITINO);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот районов, светлая/темная темы")
    public void shouldSeeDistrictScreenshot() {
        openDistricts();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        openDistricts();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста районов, светлая/темная темы")
    public void shouldSeeDistrictSuggestScreenshot() {
        openDistricts();
        basePageSteps.onListingPage().searchBar().fillSearchInput(MITINO);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        openDistricts();
        basePageSteps.onListingPage().searchBar().fillSearchInput(MITINO);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот пресетов регионов, светлая/темная темы")
    public void shouldSeeRegionPresetsScreenshot() {
        basePageSteps.onListingPage().region().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().region().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста региона, светлая/темная темы")
    public void shouldSeeRegionSuggestScreenshot() {
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().input().sendKeys(SANKT_PETERBURG);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().input().sendKeys(SANKT_PETERBURG);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private void openSubwayMap() {
        basePageSteps.onListingPage().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
    }

    private void openDistricts() {
        basePageSteps.onListingPage().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
    }

}
