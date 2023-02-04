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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mobile.element.FiltersPopup.REGION;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.LIGHT_THEME;

@Epic(GEO_SUGGEST_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты саджеста регионов")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RegionSuggestScreenshotTest {

    private static final String SUGGEST_INPUT = "Сан";

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
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот пресетов саджеста регионов, светлая/темная темы")
    public void shouldSeePresetsRegionSuggestScreenshot() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста регионов, светлая/темная темы")
    public void shouldSeeRegionSuggestScreenshot() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        basePageSteps.onListingPage().popup(REGION).input().sendKeys(SUGGEST_INPUT);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup(REGION).title().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        basePageSteps.onListingPage().popup(REGION).input().sendKeys(SUGGEST_INPUT);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup(REGION).title().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
