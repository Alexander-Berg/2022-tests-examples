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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Редактирование цены и скидок")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class EditOfferPriceDiscountTruckUsedTest {

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
                stub("cabinet/DealerCampaigns"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DesktopClientsGet/Dealer"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/UserOffersTrucksUsed"),
                stub("cabinet/UserOffersTrucksPrice")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(TRUCKS).path(USED).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Редактирование цены в сниппете б.у. ТС")
    public void shouldChangePriceInUsedSnippet() {
        steps.onCabinetOffersPage().snippet(0).priceBlock().editIcon().click();
        steps.onCabinetOffersPage().priceDiscountPopup().input("Цена без скидок", "500001");
        steps.onCabinetOffersPage().priceDiscountPopup().button("Применить").click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText("Цена успешно обновлена"));
        steps.onCabinetOffersPage().snippet(0).priceBlock().should(hasText("500 001 ₽"));
    }
}
