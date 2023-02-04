package ru.auto.tests.mobile.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@DisplayName("Про авто - страница истории автомобиля")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryPaidTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxReportRawVinPaid",
                "desktop/CarfaxReportRawLicensePlatePaid").post();

        urlSteps.testing().path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие истории по VIN. Купленная")
    public void shouldOpenHistoryByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", "4S2CK58D924333406");
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReport().status()
                .should(hasText("Проверено 40 из 43 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().repairCalculations().getRepairCalculation(0)
                .should(hasText("50 000 — 100 000 ₽\n3 марта 2013")).hover().click();
        basePageSteps.onHistoryPage().vinReport().repairCalculations().getRepairCalculation(0)
                .waitUntil(hasText("50 000 — 100 000 ₽\n3 марта 2013\nОбщая стоимость\n50 000 — 100 000 ₽\n" +
                        "Покраска\n0 — 10 000 ₽\nРабота\n0 — 10 000 ₽\nЗапчасти\n50 000 — 100 000 ₽\nТотальный ущерб\n" +
                        "Нет\nИсточник\nAudatex\nОкраска новой детали\nБАМПЕР ЗОКРАСКА НОВ ДЕТ ЭТ I\nВспомогитательные " +
                        "работы\nЗАДНИЕ ФОНАРИ - ОБА - С/У\nЗамена\nПОДКРЫЛОК З Л - С/У\nПОДКРЫЛОК З ПР - С/У\n" +
                        "БАМПЕР З - С/У\nБАМПЕР З - ЗАМ (СНЯТ)\nКОЛЕСО З Л - С/У\nКОЛЕСО З ПР - С/У\n" +
                        "КОЛЕСО ЗАДНЕЕ С/У (ЗАМЕНА)\nБАМПЕР ЗEHY05022XA8N\nНАКЛ БУКС ПРОУШ ЗД ЛEGY150EL1:\n" +
                        "ЗАЩ БКС ПРОУШ З ПРEGY150EK1:\nКРЕП НАБОР БАМПЕР ЗСМОТРИ ПО ЧАСТ\nБОЛТ БАМПЕРА З9CF600516B\n" +
                        "ПИСТОН БАМПЕРА ЗC23550ES1\nПИСТОН БАМПЕРА ЗGK2E501K5A\nГАЙКА БАМПЕРА ЗДН999100501\n" +
                        "ЗАКЛЕПКАEG21500Z1A\nФОНАРЬ З Л В СБE22151160C\nФОНАРЬ З ПР В СБE22151150C\nПР-ТУМ ФОНАРЬ " +
                        "З ЛEHY151660\nПР-ТУМ ФОНАРЬ З ПРEHY251650\nОТРАЖАТ Л БАМПЕР ЗBN8P515M0C\nОТРАЖАТ ПР " +
                        "БАМПЕР ЗBN8P515L0C\nПОДКРЫЛОК З ЛEH14561J1A\nПОДКРЫЛОК З ПРEH14561H1A"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие истории по гос номеру. Купленная")
    public void shouldOpenHistoryByNumber() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", "Y151BB178");
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReport().status()
                .should(hasText("Проверено 84 из 89 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
    }
}
