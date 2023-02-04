package ru.auto.tests.cabinet.listing;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.EXCLUDE_TAG;
import static ru.auto.tests.desktop.consts.QueryParams.TAG;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фильтр «С доставкой»")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class DeliveryTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/UserOffersCarsUsedDelivery"),
                stub("cabinet/UserOffersCarsUsedExcludeDelivery"),
                stub("cabinet/UserOffersCarsCount")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
        steps.onCabinetOffersPage().salesFiltersBlock().button("Все параметры").click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтр «С доставкой»")
    public void shouldSeeOffersWithDelivery() {
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Доставка", "С доставкой 1");
        urlSteps.addParam(TAG, "delivery").shouldNotSeeDiff();

        steps.onCabinetOffersPage().salesFiltersBlock().select("С доставкой").waitUntil(isDisplayed());
        steps.onCabinetOffersPage().snippets().waitUntil(hasSize(1));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтр «Без доставки»")
    public void shouldSeeOffersWithoutDelivery() {
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Доставка", "Без доставки 1");
        urlSteps.addParam(EXCLUDE_TAG, "delivery").shouldNotSeeDiff();

        steps.onCabinetOffersPage().salesFiltersBlock().select("Без доставки").waitUntil(isDisplayed());
        steps.onCabinetOffersPage().snippets().waitUntil(hasSize(1));
    }
}
