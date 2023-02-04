package ru.yandex.general.feed;

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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;

@Epic(FEEDS_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты страницы фидов на разных разрешениях экрана")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FeedEmptyPageScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public int width;

    @Parameterized.Parameters(name = "Скриншот страницы фидов на разных разрешениях экрана")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {1920},
                {1366}
        });
    }

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        compareSteps.resize(width, 1500);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MY).path(FEED);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы фидов на разных разрешениях экрана, светлая тема")
    public void shouldSeeFeedPageScreenshot() {
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы фидов на разных разрешениях экрана, темная тема")
    public void shouldSeeFeedPageDarkThemeScreenshot() {
        basePageSteps.setDarkThemeCookie();
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
