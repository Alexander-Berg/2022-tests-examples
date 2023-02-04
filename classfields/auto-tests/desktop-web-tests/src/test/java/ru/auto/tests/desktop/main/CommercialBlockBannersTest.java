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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;

@DisplayName("Главная - блок «Коммерческий транспорт» - баннеры")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CommercialBlockBannersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String bannerText;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameter(2)
    public String param;

    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Фургоны", LCV, "light_truck_type", "ALL_METAL_VAN"},
                {"Самосвалы", TRUCK, "truck_type", "TIPPER"},
                {"Рефрижераторы", TRUCK, "truck_type", "REFRIGERATOR"},
                {"Микроавтобусы", LCV, "light_truck_type", "MINIBUS"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        basePageSteps.setWideWindowSize(HEIGHT_1024);
        urlSteps.testing().path(MOSKVA).open();
        waitForBanner();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по баннеру")
    public void shouldClickBanner() {
        basePageSteps.onMainPage().commercialBlock().banner().hover().click();
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(param, paramValue)
                .addParam("from", "index").shouldNotSeeDiff();
    }

    @Step("Рефрешим страницу, пока не увидим нужный баннер")
    private void waitForBanner() {
        await().ignoreExceptions().atMost(60, SECONDS).pollInterval(3, SECONDS)
                .until(() -> {
                    basePageSteps.refresh();
                    return basePageSteps.onMainPage().commercialBlock().bannerTitle().getText().equals(bannerText);
                });
    }
}
