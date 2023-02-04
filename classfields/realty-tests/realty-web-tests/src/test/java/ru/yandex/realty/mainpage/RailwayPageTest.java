package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.PETERHOF;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.RAILWAY;
import static ru.yandex.realty.consts.Pages.RAILWAYS;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Страница станций")
@Feature(MAIN)
@Link("https://st.yandex-team.ru/VERTISTEST-2140")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class RailwayPageTest {

    private static final String TEST_STATION_NAME = "Новый Петергоф";
    private static final String TEST_STATION_NUMBER = "/75512/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeStationClick() {
        urlSteps.testing().path(SPB_I_LO).path(RAILWAYS).open();
        basePageSteps.onRailwaysPage().link(TEST_STATION_NAME).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(PETERHOF).path(RAILWAY).path(TEST_STATION_NUMBER)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeBreadcrumbClick() {
        urlSteps.testing().path(SPB_I_LO).path(RAILWAY).path(TEST_STATION_NUMBER).open();
        basePageSteps.onRailwaysPage().link("Недвижимость по станциям пригородных поездов").click();
        urlSteps.testing().path(PETERHOF).path(RAILWAYS).shouldNotDiffWithWebDriverUrl();
    }
}
