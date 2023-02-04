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
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Редактирование цены и скидок")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class EditOfferPriceDiscountTruckNewTest {

    private static final String LEASING = "30";
    private static final String COMMON = "80";

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
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/DealerCampaigns"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DesktopClientsGet/Dealer"),
                stub("cabinet/UserOffersTrucksNew"),
                stub("cabinet/UserOffersTrucksPrice"),
                stub("cabinet/UserOffersTrucksAttribute")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(TRUCKS).path(NEW).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Редактирование цены в сниппете нового ТС")
    public void shouldChangePriceInNewSnippet() {
        steps.onCabinetOffersPage().snippet(0).priceBlock().editIcon().click();
        steps.onCabinetOffersPage().priceDiscountPopup().input("Цена без скидок", "500001");
        steps.onCabinetOffersPage().priceDiscountPopup().button("Применить").click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText("Цена успешно обновлена"));
        steps.onCabinetOffersPage().snippet(0).priceBlock().should(hasText("500 001 ₽"));
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Редактирование скидок в новых Т.С.")
    public void ChangeDiscountInSnippet() {
        steps.onCabinetOffersPage().snippet(0).priceBlock().editIcon().click();
        steps.onCabinetOffersPage().priceDiscountPopup().input("Скидка по лизингу", LEASING);
        steps.onCabinetOffersPage().priceDiscountPopup().input("Размер максимальной скидки", COMMON);
        steps.onCabinetOffersPage().priceDiscountPopup().button("Применить").click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText("Скидки успешно обновлены"));
        steps.onCabinetOffersPage().snippet(0).priceBlock().should(hasText("98 989 ₽\nСкидки"));
    }
}
