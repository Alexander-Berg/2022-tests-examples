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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(LISTING_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты листинга")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingScreenshotTest {

    private static final Set<String> IGNORE = newHashSet("//div[contains(@class, 'NavigationWithBanner')]");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public int width;

    @Parameterized.Parameter(1)
    public String theme;

    @Parameterized.Parameters(name = "Скриншот листинга на разных разерешениях. Тема «{1}»")
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
        mockRule.graphqlStub(mockResponse()
                .setCategoryListingExample()
                .setRegionsTemplate()
                .setCategoriesTemplate()
                .build()).withDefaults().create();
        compareSteps.resize(width, 1500);
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот листинга на разных разрешениях экрана, светлая/темная темы")
    public void shouldSeeListingScreenshot() {
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreasWithBorders(
                basePageSteps.onBasePage().pageRoot(),
                basePageSteps.onListingPage().adBannerNavigationBlock());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreasWithBorders(
                basePageSteps.onBasePage().pageRoot(),
                basePageSteps.onListingPage().adBannerNavigationBlock());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот прилипшего хэдера на листинге на разных разрешениях экрана, светлая/темная темы")
    public void shouldSeeFloatedHeaderListingScreenshot() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreasWithScrollOffset(
                basePageSteps.onBasePage().pageRoot(),
                PXLS_TO_FLOAT_HEADER,
                basePageSteps.onListingPage().adBannerNavigationBlock());

        urlSteps.setProductionHost().open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreasWithScrollOffset(
                basePageSteps.onBasePage().pageRoot(),
                PXLS_TO_FLOAT_HEADER,
                basePageSteps.onListingPage().adBannerNavigationBlock());


        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
