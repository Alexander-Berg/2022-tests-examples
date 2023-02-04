package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.KUPIT_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

@DisplayName("Базовые фильтры. Скриншот фильтров")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ScreenshotFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE)).createWithDefaults();
        compareSteps.resize(320, 4000);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот базовых фильтров")
    public void shouldSeeFilterScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().searchFilters());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().searchFilters());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот базовых фильтров с типом сделки")
    public void shouldSeeTypeFilterScreenshot() {
        basePageSteps.onMobileMainPage().searchFilters().button(KUPIT_OPTION).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().searchFilters());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.onMobileMainPage().searchFilters().button(KUPIT_OPTION).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().searchFilters());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот базовых фильтров с типом недвижимости")
    public void shouldSeeDealFilterScreenshot() {
        basePageSteps.onMobileMainPage().searchFilters().button(KVARTIRU_OPTION).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().searchFilters());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.onMobileMainPage().searchFilters().button(KVARTIRU_OPTION).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().searchFilters());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот расширенных фильтров")
    public void shouldSeeExtendedFilterScreenshot() {
        basePageSteps.onMobileMainPage().openExtFilter();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().extendFilters());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.onMobileMainPage().openExtFilter();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMainPage().extendFilters());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
