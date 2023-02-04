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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.calls.Filters.HIDE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Каунтер фильтров по офферам")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsOfferFiltersCountersTest {

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
                "cabinet/Calltracking",
                "cabinet/CalltrackingSettings",
                "cabinet/CalltrackingAggregated").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
        basePageSteps.onCallsPage().filters().allParameters().click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем один селект фильтр, скрываем - каунтер = «1»")
    public void shouldSeeCounterForOneSelectFilter() {
        basePageSteps.onCallsPage().filters().offerFilters().selectItem("Кузов", "Седан ");
        basePageSteps.onCallsPage().filters().button(HIDE).click();

        basePageSteps.onCallsPage().filters().counter().should(hasText("1"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем один инпут фильтр, скрываем - каунтер = «1»")
    public void shouldSeeCounterForOneInputFilter() {
        basePageSteps.onCallsPage().filters().offerFilters().input("VIN", "XWEGN412BF0004265");
        basePageSteps.onCallsPage().filters().button(HIDE).click();

        basePageSteps.onCallsPage().filters().counter().should(hasText("1"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем несколько фильтров, скрываем - каунтер = «4»")
    public void shouldSeeCounterForSeveralFilters() {
        fillFourFilters();
        basePageSteps.onCallsPage().filters().button(HIDE).click();

        basePageSteps.onCallsPage().filters().counter().should(hasText("4"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем несколько фильтров, сбрасываем - каунтер не отображается")
    public void shouldSeeResetSeveralFilters() {
        fillFourFilters();
        basePageSteps.onCallsPage().filters().button(HIDE).click();
        basePageSteps.onCallsPage().filters().resetButton().click();

        basePageSteps.onCallsPage().filters().counter().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

    private void fillFourFilters() {
        basePageSteps.onCallsPage().filters().offerFilters().selectItem("Год от", "2018");
        basePageSteps.onCallsPage().filters().offerFilters().selectItem("Коробка", "Механическая ");
        basePageSteps.onCallsPage().filters().offerFilters().input("Цена от, ₽", "20000");
        basePageSteps.onCallsPage().filters().offerFilters().input("до", "500000");
    }

}
