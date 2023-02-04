package ru.auto.tests.embed.vin;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES_WIGET;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.EMBED;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_PROMO;
import static ru.auto.tests.desktop.consts.Pages.VIN;

@Feature(SALES_WIGET)
@DisplayName("EMBED: виджет отчета по VIN")
@Owner(NATAGOLOVKINA)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)

public class WidgetVinTest {

    private static final String VIN_SUCC = "4S2CK58D924333406/";
    private static final String VIN_ERR = "123";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String vin_theme;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"autoru"},
                {"autolada"},
                {"audiClub"},
                {"bmwClub"},
                {"autoladaMobile"},
                {"autoruMag"},
                {"landCruiser"},
                {"ladaKalinaClub"},
                {"toyotaLandCruiserPradoClub"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/CarfaxReportRawVinNotPaid").post();

        urlSteps.subdomain(SUBDOMAIN_PROMO).path(EMBED).path(VIN)
                .addParam("_debug_embed", "true")
                .addParam("theme", vin_theme).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск по корректному VIN номеру")
    @Owner(NATAGOLOVKINA)
    public void shouldFindVinReport() {
        basePageSteps.onVinSearchPage().input().sendKeys(VIN_SUCC);
        basePageSteps.onVinSearchPage().button().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(HISTORY).path(VIN_SUCC)
                .addParam("from", "widget.vin." + vin_theme.toLowerCase()).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск по некорректному VIN номеру")
    @Owner(NATAGOLOVKINA)
    public void shouldNotFindVinReport() {
        basePageSteps.onVinSearchPage().input().sendKeys(VIN_ERR);
        basePageSteps.onVinSearchPage().button().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(HISTORY)
                .addParam("from", "widget.vin." + vin_theme.toLowerCase()).shouldNotSeeDiff();
    }

}