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
import static ru.yandex.general.consts.Pages.CHELYABINSK;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.element.FiltersPopup.ADDRESS_AND_RADIUS;
import static ru.yandex.general.mobile.element.FiltersPopup.METRO;
import static ru.yandex.general.mobile.page.ListingPage.DISTRICT;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.LIGHT_THEME;

@Epic(GEO_SUGGEST_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты саджеста")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SuggestScreenshotTest {

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
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста адреса")
    public void shouldSeeAddressScreenshot() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        openAddressSuggest();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        urlSteps.setProductionHost().open();
        openAddressSuggest();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста метро, светлая/темная темы")
    public void shouldSeeSubwaySuggestScreenshot() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        openSubwaySuggest();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        urlSteps.setProductionHost().open();
        openSubwaySuggest();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот саджеста районов, светлая/темная темы")
    public void shouldSeeDistrictSuggestScreenshot() {
        urlSteps.testing().path(CHELYABINSK).path(ELEKTRONIKA).open();
        openDistrictSuggest();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        urlSteps.setProductionHost().open();
        openDistrictSuggest();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private void openAddressSuggest() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys("За");
    }

    private void openSubwaySuggest() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
    }

    private void openDistrictSuggest() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
    }

}
