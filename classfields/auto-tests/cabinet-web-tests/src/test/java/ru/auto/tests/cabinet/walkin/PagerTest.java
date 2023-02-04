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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALK_IN;

@Feature(CABINET_DEALER)
@DisplayName("Приезды в салон - пагинация")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class PagerTest {
    private static final String SECOND_PAGE = "2";
    private static final String FIRST_PAGE = "1";
    private static final int PAGE_SIZE = 10;

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
                "cabinet/DealerWalkInEvents",
                "cabinet/DealerTariff/CarsUsedOn").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).open();

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthDealer",
                        "cabinet/ApiAccessClient",
                        "cabinet/CommonCustomerGet",
                        "cabinet/DealerWalkInStats",
                        "cabinet/DealerWalkInEventsPage2",
                        "cabinet/DealerTariff/CarsUsedOn")
                .post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickShowMoreButton() {
        steps.onCabinetWalkInPage().eventList().should(hasSize(PAGE_SIZE));
        steps.onCabinetWalkInPage().pager().button("Показать ещё").click();
        steps.onCabinetWalkInPage().eventList().should(hasSize(PAGE_SIZE * 2));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке «Следующая»")
    public void shouldClickNextButton() {
        steps.onCabinetWalkInPage().pager().next().click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).addParam("page", SECOND_PAGE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке «Предыдущая»")
    public void shouldClickPrevButton() {
        urlSteps.addParam("page", SECOND_PAGE).open();

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthDealer",
                        "cabinet/ApiAccessClient",
                        "cabinet/DealerWalkInStats",
                        "cabinet/DealerWalkInEvents",
                        "cabinet/DealerTariff/CarsUsedOn")
                .post();

        steps.onCabinetWalkInPage().pager().prev().click();
        urlSteps.replaceParam("page", FIRST_PAGE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Переход на страницу «2»")
    public void shouldBeReturnedToFirstPageFromSecondPage() {
        steps.onCabinetWalkInPage().pager().page(SECOND_PAGE).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).addParam("page", SECOND_PAGE).shouldNotSeeDiff();
    }
}
