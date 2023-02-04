package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;

@DisplayName("Баннер «Отчёты по VIN и госномеру»")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RotationBannerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String bannerText;

    @Parameterized.Parameter(1)
    public String bannerUrl;

    @Parameterized.Parameters(name = "name = {index}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Электромобили\nКак и зачем их покупать", "%s/electro/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SharkCreditProductList").post();

        urlSteps.testing().open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по баннеру")
    public void shouldClickBanner() {
        waitForBanner();
        basePageSteps.onMainPage().marksBlock().banner().click();
        urlSteps.fromUri(format(bannerUrl, urlSteps.getConfig().getTestingURI())).shouldNotSeeDiff();
    }

    @Step("Рефрешим страницу, пока не увидим нужный баннер")
    private void waitForBanner() {
        await().ignoreExceptions().atMost(60, SECONDS).pollInterval(3, SECONDS)
                .until(() -> {
                    basePageSteps.refresh();
                    return basePageSteps.onMainPage().marksBlock().banner().getText().equals(bannerText);
                });
    }
}
