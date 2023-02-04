package ru.auto.tests.cabinet.agency.listing;

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
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_APPLIED;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_CANCELED;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AGENCY_CABINET)
@DisplayName("Кабинет агента. Листинг")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ListingScreenshotAgencyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionAgencyClient"),
                stub("cabinet/AgencyAgencyGetClientId"),
                stub("cabinet/AgencyClientsGet"),
                stub("cabinet/ApiAccessClientAgency"),
                stub("cabinet/DealerAccountAgencyClient"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/CommonCustomerGetAgencyClient"),
                stub("cabinet/UserOffersCarsAgency"),
                stub("cabinet/AgencyClientSidebarGet")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(SALES).addParam(CLIENT_ID, "25718").open();
        waitSomething(2, TimeUnit.SECONDS);
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключаем и отключаем услугу «Премиум»")
    public void shouldActivateAndCancelAgencyService() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsPremiumPost"),
                stub("cabinet/UserOffersCarsProductsPremiumDelete")).update();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().premium().click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().premium().click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_CANCELED));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Редактировать»")
    public void shouldSeeAgencyOfferEditPage() {
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Редактировать").click();
        steps.switchToNextTab();
        urlSteps.testing().path(BETA).path(CARS).path(NEW).path(EDIT).path("/1076842087-f1e84/")
                .addParam(CLIENT_ID, "25718").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Удаление оффера")
    public void shouldSeeRemovedAgencyOffer() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsDelete")).update();

        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Удалить").click();
        steps.onCabinetOffersPage().removePopup().button("Да").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление удалено"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Снятие с продажи")
    public void shouldHideAgencyOffer() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsHide")).update();

        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Снять с\u00a0продажи").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление снято с продажи"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Активировать оффер")
    public void shouldActivateAgencyOffer() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsCarsInactiveAgency"),
                stub("cabinet/UserOffersCarsActivate")).update();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(SALES).addParam(CLIENT_ID, "25718")
                .addParam(STATUS, "inactive").open();
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Активировать").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление активировано"));
    }
}
