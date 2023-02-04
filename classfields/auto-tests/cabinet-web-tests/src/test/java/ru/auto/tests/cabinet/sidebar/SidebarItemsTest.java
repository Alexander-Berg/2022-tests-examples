package ru.auto.tests.cabinet.sidebar;

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
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;

//import io.qameta.allure.Parameter;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Боковое меню")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SidebarItemsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Меню")
    @Parameterized.Parameter
    public String item;

    //@Parameter("Ожидаемый path")
    @Parameterized.Parameter(1)
    public String expectedPath;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"Кошелек", WALLET},
                {"Тарифы", CALCULATOR},
                {"Салон", CARD},
                {"Звонки", CALLS},
                {"Фиды", FEEDS},
                {"Настройки", SETTINGS}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DesktopSidebarGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Переход с меню")
    public void shouldMenuItemPage() {
        steps.onCabinetOffersPage().sidebar().item(item).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(expectedPath).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Переход с выпадающего подменю")
    public void shouldMenuItemOnCollapsedSidebarPage() {
        steps.collapseSidebar();
        steps.onCabinetOffersPage().sidebar().item(item).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(expectedPath).shouldNotSeeDiff();
    }
}
