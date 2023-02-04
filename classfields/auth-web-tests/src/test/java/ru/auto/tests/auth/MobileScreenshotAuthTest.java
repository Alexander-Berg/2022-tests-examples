package ru.auto.tests.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;

@DisplayName("Адаптированный тест. Отображение страницы авторизации")
@Feature(AUTH)
@Story("Mobile auth")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class MobileScreenshotAuthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(urlSteps.getConfig().getMobileURI())).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Отображение страницы")
    public void shouldSeeBlock() {
        screenshotSteps.setWindowSize(1920, 5000);

        Screenshot testingScreenshot = screenshotSteps.
                getElementScreenshotWithWaiting(basePageSteps.onAuthPage().authForm());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.
                getElementScreenshotWithWaiting(basePageSteps.onAuthPage().authForm());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

}
