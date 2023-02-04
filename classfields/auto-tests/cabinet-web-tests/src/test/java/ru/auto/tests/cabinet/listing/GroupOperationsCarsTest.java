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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_APPLIED;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_CANCELED;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.PREMIUM_ACTIVE;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.SPEC_ACTIVE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Групповые операции")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class GroupOperationsCarsTest {

    private static final String YES = "Да";

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
                stub("cabinet/UserOffersCarsUsed")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).addParam(STATUS, "active").open();
        steps.onCabinetOffersPage().salesFiltersBlock().groupOperationCheckbox().click();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключаем и отключаем услугу «Спец»")
    public void shouldActivateAndDeactivateSpecService() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsSpecialPost"),
                stub("cabinet/UserOffersCarsProductsSpecialDelete")).update();

        steps.onCabinetOffersPage().groupServiceButtons().special().click();
        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().special()
                        .should(hasClass(containsString(SPEC_ACTIVE))));

        steps.onCabinetOffersPage().groupServiceButtons().special().click();
        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText(SERVICE_CANCELED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().special()
                        .should(hasClass(not(containsString(SPEC_ACTIVE)))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключаем и отключаем услугу «Премиум»")
    public void shouldActivateAndDeactivatePremiumService() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsPremiumPost"),
                stub("cabinet/UserOffersCarsProductsPremiumDelete")).update();

        steps.onCabinetOffersPage().groupServiceButtons().premium().click();
        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().premium()
                        .should(hasClass(containsString(PREMIUM_ACTIVE))));

        steps.onCabinetOffersPage().groupServiceButtons().premium().click();
        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText(SERVICE_CANCELED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().premium()
                        .should(hasClass(not(containsString(PREMIUM_ACTIVE)))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Клик по кнопке услугу «Бронирование»")
    public void shouldClickBookButton() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsBookingAllowedFalsePut")).update();

        steps.onCabinetOffersPage().groupActionsButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Бронирование").click();
        steps.onCabinetOffersPage().popup().waitUntil(hasText("Вы действительно хотите сделать недоступными " +
                "для бронирования 2 объявления?\nДа\nНет"));

        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed())
                .should(hasText("Объявления теперь недоступны для бронирования"));
    }
}
