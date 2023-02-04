package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.RAILWAY;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Страница станций")
@Feature(MAIN)
@Link("https://st.yandex-team.ru/VERTISTEST-2140")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RailwayPageCategoriesClickTest {

    private static final String TEST_STATION_NUMBER = "/75512/";
    private static final String TEST_REGION_PATH = "/petergof/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private MockRuleConfigurable mockRuleConfigurable;

    @Parameterized.Parameter
    public String block;

    @Parameterized.Parameter(1)
    public String link;

    @Parameterized.Parameter(2)
    public String path;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Купить загородную", "Купить дом", "/kupit/dom/railway-novyj-petergof-75512/"},
                {"Купить квартиру", "1-комнатные", "/kupit/kvartira/odnokomnatnaya/railway-novyj-petergof-75512/"},
                {"Снять квартиру", "1-комнатные", "/snyat/kvartira/odnokomnatnaya/railway-novyj-petergof-75512/"},
                {"Новостройки", "C парковкой", "/kupit/novostrojka/railway-novyj-petergof-75512/s-parkovkoy/"},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeStationClick() {
        mockRuleConfigurable.linksRailway().createWithDefaults();
        urlSteps.testing().path(SPB_I_LO).path(RAILWAY).path(TEST_STATION_NUMBER).open();
        basePageSteps.onRailwaysPage().block(block).link(link).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(TEST_REGION_PATH).path(path).shouldNotDiffWithWebDriverUrl();
    }
}
