package ru.auto.tests.cabinet.placement_report;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.PLACEMENT_REPORT;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Отчёт о размещении")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class PlacementReportTest {

    public static final String DATE_FROM = "2021-06-30";
    public static final String DATE_TO = "2021-07-30";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/SessionDirectDealerAristos",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerWarehouseDailyState",
                "cabinet/DealerOffersDailyStatsFull",
                "cabinet/DealerTariffAllNew",
                "cabinet/DealerWalkInStatsCarsUsed",
                "cabinet/ApiAccessClient",
                "cabinet/DealerInfoMultipostingEnabled",
                "cabinet/CalltrackingSettingsWithColltrackingEnabled",
                "cabinet/DealerWarehouseUniqueStats",
                "cabinet/CalltrackingAggregatedAutoru",
                "cabinet/CalltrackingAggregatedAvito",
                "cabinet/CalltrackingAggregatedDrom").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PLACEMENT_REPORT).addParam("from", DATE_FROM).addParam("to", DATE_TO).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class})
    @DisplayName("Должны поменять дату отчета")
    public void shouldChangeReportDates() {
        steps.onCabinetPlacementReportPage().calendarButton().should(isDisplayed()).click();
        steps.onCabinetPlacementReportPage().calendar().selectPeriod("суббота, 3 июля 2021", "среда, 14 июля 2021");

        steps.onCabinetPlacementReportPage().calendarButton().should(hasText("3 — 14 июля"));
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PLACEMENT_REPORT)
                .addParam("from", "2021-07-03").addParam("to", "2021-07-14")
                .addParam("category", "CARS").addParam("section", "USED")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class})
    @DisplayName("Должны кликнуть в кнопку экспорта отчета")
    public void shouldClickExportButton() {
        steps.onCabinetPlacementReportPage().exportButton().should(isDisplayed()).click();
        steps.onCabinetPlacementReportPage().notifier().waitUntil(isDisplayed(), 20)
                .should(hasText("Отчёт был успешно загружен"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class})
    @DisplayName("Должны увидеть подсказки к названию столбца таблички")
    public void shouldSeePlatformTableTitleTooltips() {
        steps.onCabinetPlacementReportPage().platformTable().tableHeader("Площадка").should(isDisplayed()).hover();
        steps.onCabinetPlacementReportPage().popup()
                .should(hasText("Сайт, на котором размещались объявления с 30 июня по 30 июля"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class})
    @DisplayName("Должны увидеть подсказки к названию графиков")
    public void shouldSeeChartTooltips() {
        steps.onCabinetPlacementReportPage().chart("Сводный график").tooltipIcon().should(isDisplayed()).hover();
        steps.onCabinetPlacementReportPage().popup()
                .should(hasText("Как менялось количество звонков и затраченных средств относительно размеров склада"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class})
    @DisplayName("Должны увидеть легенду у графика «Приезды в салон с Авто.ру»")
    public void shouldSeeWalkInLegend() {
        steps.onCabinetPlacementReportPage().chart("Приезды в салон с Авто.ру").walkInCollapseButton()
                .should(isDisplayed()).click();

        steps.onCabinetPlacementReportPage().chart("Приезды в салон с Авто.ру").walkInLegend()
                .should(hasText("0\nпосетителей\nПосетители дилерского центра, по которым определены данные по полу, " +
                        "возрасту, истории просмотров.\nДанные по полу и возрасту посетителей могут иметь погрешность " +
                        "в пределах 2%."));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class})
    @DisplayName("Должны увидеть графики")
    public void shouldSeeCharts() {
        steps.onCabinetPlacementReportPage().chart("Сводный график").should(isDisplayed()).should(hasText("Сводный график" +
                "\nЗвонки 9\nБыло в продаже 256\n₽ на размещение 0 ₽\n₽ на услуги 0 ₽\n30.06\n02.07\n04.07\n06.07\n08.07" +
                "\n10.07\n12.07\n14.07\n16.07\n18.07\n20.07\n22.07\n24.07\n26.07\n28.07\n30.07\n1\n3\n6\n25\n75\n150\n4" +
                "\n2\nВсе\nАвто.ру\nАвито\nДром"));

        steps.onCabinetPlacementReportPage().chart("Склад").should(isDisplayed()).should(hasText("Склад\nБыло в продаже " +
                "256\nСнято с продажи 14\n30.06\n02.07\n04.07\n06.07\n08.07\n10.07\n12.07\n14.07\n16.07\n18.07\n20.07\n" +
                "22.07\n24.07\n26.07\n28.07\n30.07\n25\n75\n150\n4"));

        steps.onCabinetPlacementReportPage().chart("Просмотры объявлений").should(isDisplayed()).should(hasText("Просмотры" +
                " объявлений\nАвто.ру 144\nАвито 0\nДром 0\n30.06\n02.07\n04.07\n06.07\n08.07\n10.07\n12.07\n14.07\n16.07" +
                "\n18.07\n20.07\n22.07\n24.07\n26.07\n28.07\n30.07\n4\n12\n20\n26"
));

        steps.onCabinetPlacementReportPage().chart("Просмотры контактов").should(isDisplayed()).should(hasText("Просмотры" +
                " контактов\nАвто.ру 10\nАвито 0\nДром 0\n30.06\n02.07\n04.07\n06.07\n08.07\n10.07\n12.07\n14.07\n16.07" +
                "\n18.07\n20.07\n22.07\n24.07\n26.07\n28.07\n30.07\n2\n6\n10\n13"));

        steps.onCabinetPlacementReportPage().chart("Звонки").should(isDisplayed()).should(hasText("Звонки\nАвто.ру 3/0\n" +
                "Авито 3/0\nДром 3/0\n30.06\n02.07\n04.07\n06.07\n08.07\n10.07\n12.07\n14.07\n16.07\n18.07\n20.07\n22.07" +
                "\n24.07\n26.07\n28.07\n30.07\n2\n4\n6\nПринятые\n5\n2\nПропущенные\nУникальный звонок\nЦелевой звонок"));

        steps.onCabinetPlacementReportPage().chart("Приезды в салон с Авто.ру").should(isDisplayed())
                .should(hasText("Приезды в салон с Авто.ру\nПосетители 0\n30.06\n02.07\n04.07\n06.07\n08.07\n10.07\n" +
                        "12.07\n14.07\n16.07\n18.07\n20.07\n22.07\n24.07\n26.07\n28.07\n30.07\n1\n2\n3\n5\n0\n" +
                        "посетителей\nПодробнее"));
    }
}
