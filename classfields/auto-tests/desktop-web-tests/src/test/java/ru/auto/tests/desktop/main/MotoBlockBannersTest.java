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
import ru.auto.tests.desktop.step.CookieSteps;
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
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;

@DisplayName("Главная - блок «Мототехника» - баннеры")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MotoBlockBannersTest {

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
    public CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Текст баннера")
    @Parameterized.Parameter
    public String bannerText;

    //@Parameter("Ссылка баннера")
    @Parameterized.Parameter(1)
    public String bannerUrl;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Городские", "%s/moskva/motorcycle/all/?moto_type=ROAD&moto_type=CLASSIC&moto_type=NAKEDBIKE&moto_type=ROAD_GROUP"},
                {"Спортивные", "%s/moskva/motorcycle/all/?moto_type=SPORTBIKE&moto_type=SPORTTOURISM&moto_type=SPORT_GROUP&moto_type=SUPERSPORT"},
                {"До 250 см³", "%s/moskva/motorcycle/all/?displacement_to=250"},
                {"Harley-Davidson", "%s/moskva/motorcycle/harley_davidson/all/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        screenshotSteps.setWindowSize(WIDTH_WIDE_PAGE, 1024);
        urlSteps.testing().path(MOSKVA).open();
        waitForBanner();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по баннеру")
    public void shouldClickBanner() {
        basePageSteps.onMainPage().motoBlock().banner().hover().click();
        urlSteps.fromUri(format(bannerUrl, urlSteps.getConfig().getTestingURI())).addParam("from", "index")
                .shouldNotSeeDiff();
    }

    @Step("Рефрешим страницу, пока не увидим нужный баннер")
    private void waitForBanner() {
        await().ignoreExceptions().atMost(120, SECONDS).pollInterval(3, SECONDS)
                .until(() -> {
                    basePageSteps.refresh();
                    return basePageSteps.onMainPage().motoBlock().bannerTitle().getText().equals(bannerText);
                });
    }
}