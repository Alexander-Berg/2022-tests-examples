package ru.auto.tests.cabinet.tradein;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRADE_IN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Трейд-ин - вкладки")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TradeInTabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String tabTitle;

    @Parameterized.Parameter(2)
    public String tabUrl;

    @Parameterized.Parameters(name = "{0}, {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"ALL", "С пробегом", "USED"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaignProducts",
                "cabinet/DealerTradeIn2019_03_19_2019_03_29",
                "cabinet/DealerTradeIn2019_03_19_2019_03_29SectionUsedPage1").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(TRADE_IN).addParam("from_date", "2019-03-19")
                .addParam("to_date", "2019-03-29").addParam("section", startUrl).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по вкладке")
    public void shouldClickTab() {
        steps.onCabinetTradeInPage().tab(tabTitle).should(isDisplayed()).click();
        urlSteps.replaceParam("section", tabUrl).addParam("page", "1").shouldNotSeeDiff();
        steps.onCabinetTradeInPage().tradeInItemsList().waitUntil(hasSize(greaterThan(0)));
    }
}
