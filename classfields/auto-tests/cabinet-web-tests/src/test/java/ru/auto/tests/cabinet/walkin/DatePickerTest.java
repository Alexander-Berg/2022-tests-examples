package ru.auto.tests.cabinet.walkin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALK_IN;
import static ru.auto.tests.desktop.step.cabinet.DateFormatter.daysAgo;
import static ru.auto.tests.desktop.step.cabinet.DateFormatter.today;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Приезды в салон - Кнопка открытия календаря")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class DatePickerTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerWalkInStats",
                "cabinet/DealerTariff/CarsUsedOn").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).addParam("from", daysAgo(7))
                .addParam("to", today()).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Выбор другой даты")
    public void shouldChangeDate() {
        steps.onCabinetWalkInPage().calendarButton().should(isDisplayed()).click();
        steps.onCabinetTradeInPage().calendar().selectPeriod(daysAgoText(8), daysAgoText(1));
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).addParam("from", daysAgo(8))
                .addParam("to", daysAgo(1)).shouldNotSeeDiff();
    }

    @Step
    public static String daysAgoText(int i) {
        LocalDate date = LocalDate.now().minusDays(i);
        Locale ruLocale = new Locale("ru", "RU");
        return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale));
    }
}
