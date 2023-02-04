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
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.PLACEMENT_REPORT;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Боковое меню")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SidebarSubmenuItemsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CabinetOffersPageSteps steps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Меню")
    @Parameterized.Parameter
    public String item;

    //@Parameter("Подменю")
    @Parameterized.Parameter(1)
    public String submenu;

    //@Parameter("Ожидаемый path")
    @Parameterized.Parameter(2)
    public String expectedPath;

    @Parameterized.Parameters(name = "{index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"Сводная", "Основные показатели", "/"},
                {"Сводная", "Отчёт о размещении", PLACEMENT_REPORT},
                {"Объявления", "Легковые новые", "/sales/cars/new/"},
                {"Объявления", "Легковые c пробегом", "/sales/cars/used/"},
                {"Объявления", "Мото", "/sales/moto/"},
                {"Объявления", "Коммерческие", "/sales/trucks/"},
                {"Салон", "Информация", CARD},
                {"Салон", "Реквизиты", "/card/details/"},
                {"Заявки", "Trade-In", "/orders/trade-in/"}
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
    @DisplayName("Переход с выпадающего подменю")
    public void shouldMenuItemOnCollapsedSidebarPage() {
        steps.moveCursor(steps.onCabinetOffersPage().sidebar().item(item));
        steps.onCabinetOffersPage().sidebar().submenuItem(submenu).waitUntil(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(expectedPath).shouldNotSeeDiff();
    }
}
