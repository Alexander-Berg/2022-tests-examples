package ru.auto.tests.cabinet.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Кабинет дилера. Звонки. Фильтры. Календарь")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsFiltersCalendarTest {

    private static final Locale LOCALE = new Locale("ru", "RU");

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
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/CommonCustomerGet",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/ApiAccessClient",
                "cabinet/CalltrackingPeriodFromTo",
                "cabinet/CalltrackingAggregatedPeriodFromTo",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregated").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор даты в календаре")
    public void shouldSelectDate() {
        LocalDate date = LocalDate.now();
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, LOCALE);
        String fromDateMachine = date.format(DateTimeFormatter.ofPattern("2020-MM-01"));
        String toDateMachine = date.format(DateTimeFormatter.ofPattern("2020-MM-20"));
        String fromDateHuman = date.format(DateTimeFormatter.ofPattern("1 MMMM", LOCALE));
        String toDateHuman = date.format(DateTimeFormatter.ofPattern("20 MMMM", LOCALE));

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).addParam("from", fromDateMachine)
                .addParam("to", toDateMachine).open();
        basePageSteps.onCallsPage().filters().calendarButton().click();
        basePageSteps.onCallsPage().filters().calendar().selectPeriod(fromDateHuman, toDateHuman);
        basePageSteps.onCallsPage().filters().calendarButton().should(hasText(format("1 — 20 %s", monthName)));
        urlSteps.replaceParam("from", fromDateMachine).replaceParam("to", toDateMachine)
                .shouldNotSeeDiff();
        basePageSteps.onCallsPage().stats().title().should(hasText(format("Статистика звонков с 1 %1$s по 20 %1$s", monthName)));
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(20));
    }
}
