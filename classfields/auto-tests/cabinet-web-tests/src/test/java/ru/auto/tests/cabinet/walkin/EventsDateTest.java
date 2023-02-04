package ru.auto.tests.cabinet.walkin;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALK_IN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Приезды в салон - дата событий в списке событий")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class EventsDateTest {

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
                "cabinet/DealerWalkInEventsPage2",
                "cabinet/DealerWalkInEvents",
                "cabinet/DealerTariff/CarsUsedOn").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Переход на страницу событий с другой датой")
    public void shouldSeeAnotherEventsDate() {
        steps.onCabinetWalkInPage().eventsDate().should(hasText("3 декабря, вторник"));
        steps.onCabinetWalkInPage().pager().page("2").click();
        steps.onCabinetWalkInPage().eventsDate().should(hasText("2 декабря, понедельник"));
        steps.onCabinetWalkInPage().getEventItem(0).waitUntil(hasText("ПК\nPeugeot 3008, с пробегом\n" +
                "С пробегом Ford Mustang\n—"));
    }
}
