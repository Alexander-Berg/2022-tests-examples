package ru.yandex.general.listing;

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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.LIGHT_THEME;
import static ru.yandex.general.mobile.step.BasePageSteps.LIST;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(LISTING_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты листинга")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingScreenshotTest {

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
        mockRule.graphqlStub(mockResponse()
                .setCategoryListingExample()
                .setRegionsTemplate()
                .setCategoriesTemplate()
                .setMetroTemplate()
                .setDistrictsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(ELEKTRONIKA);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот листинга, светлая/темная темы")
    public void shouldSeeListingScreenshot() {
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот листинга списком, светлая/темная темы")
    public void shouldSeeListListingScreenshot() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
