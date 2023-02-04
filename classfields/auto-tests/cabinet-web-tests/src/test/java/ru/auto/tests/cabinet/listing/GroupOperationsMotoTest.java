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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_APPLIED;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_CANCELED;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.FRESH_ACTIVE;
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
public class GroupOperationsMotoTest {

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
                stub("cabinet/UserOffersMotoUsed")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(MOTO).path(USED).addParam(STATUS, "active").open();
        steps.onCabinetOffersPage().salesFiltersBlock().groupOperationCheckbox().click();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключаем и отключаем услугу «Спец»")
    public void shouldActivateAndDeactivateSpecService() {
        mockRule.setStubs(stub("cabinet/UserOffersMotoProductsSpecialPost"),
                stub("cabinet/UserOffersMotoProductsSpecialDelete")).update();

        steps.onCabinetOffersPage().groupServiceButtons().special().click();
        steps.onCabinetOffersPage().popup().button(YES).click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().special().waitUntil(hasClass(containsString(SPEC_ACTIVE))));

        steps.onCabinetOffersPage().groupServiceButtons().special().click();
        steps.onCabinetOffersPage().popup().button(YES).click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_CANCELED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().special().should(hasClass(not(containsString(SPEC_ACTIVE)))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключаем и отключаем услугу «Премиум»")
    public void shouldActivateAndDeactivatePremiumService() {
        mockRule.setStubs(stub("cabinet/UserOffersMotoProductsPremiumPost"),
                stub("cabinet/UserOffersMotoProductsPremiumDelete")).update();

        steps.onCabinetOffersPage().groupServiceButtons().premium().click();
        steps.onCabinetOffersPage().popup().button(YES).click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().premium().waitUntil(hasClass(containsString(PREMIUM_ACTIVE))));

        steps.onCabinetOffersPage().groupServiceButtons().premium().click();
        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_CANCELED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().premium()
                        .should(hasClass(not(containsString(PREMIUM_ACTIVE)))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключаем услугу «Поднять»")
    public void shouldActivateRaiseService() {
        mockRule.setStubs(stub("cabinet/UserOffersMotoProductsFreshPost")).update();

        steps.onCabinetOffersPage().groupServiceButtons().fresh().click();
        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.serviceButtons().fresh().should(hasClass(containsString(FRESH_ACTIVE))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Снимаем офферы с продажи")
    public void shouldWithdrawnFromSaleOffers() {
        mockRule.setStubs(stub("cabinet/UserOffersMotoHide")).update();

        steps.onCabinetOffersPage().groupActionsButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Снять с продажи").click();
        steps.onCabinetOffersPage().popup().button(YES).click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявления сняты с продажи"));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.sale().should(hasClass(containsString("OfferSnippetRoyal_disabled"))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Удаляем офферы")
    public void shouldRemovedOffers() {
        mockRule.setStubs(stub("cabinet/UserOffersMotoDelete")).update();

        steps.onCabinetOffersPage().groupActionsButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Удалить").click();
        steps.onCabinetOffersPage().removePopup().button(YES).click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявления удалены"));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.sale().should(hasClass(containsString("OfferSnippetRoyal_disabled"))));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Отмена удаления оффера")
    public void shouldSeeCanceledRemoveOffer() {
        steps.onCabinetOffersPage().groupActionsButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Удалить").click();
        steps.onCabinetOffersPage().removePopup().button("Нет").click();

        steps.onCabinetOffersPage().removePopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippets().should(not(empty()));
        steps.onCabinetOffersPage().snippets().forEach(snippet ->
                snippet.sale().should(hasClass(not(containsString("OfferSnippetRoyal_disabled")))));
    }
}
