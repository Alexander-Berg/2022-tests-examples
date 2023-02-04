package ru.yandex.general.listing;

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
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.general.page.ListingPage.FILTERS;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(LISTING_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты контролов листинга")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingControlsScreenshotTest {

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
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.resize(1920, 1500);
        urlSteps.testing().path(NOUTBUKI).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот попапа сортировки на листинге, светлая/темная темы")
    public void shouldSeeSortPopupScreenshot() {
        basePageSteps.onListingPage().filters().sortButton().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().filters().sortButton().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот попапа «Все фильтры» на листинге, светлая/темная темы")
    public void shouldSeeAllFiltersPopupScreenshot() {
        basePageSteps.onListingPage().filter(FILTERS).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().modal().content());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().filter(FILTERS).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().modal().content());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
