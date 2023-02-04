package ru.auto.tests.cabinet.dashboard;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 24.01.19
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Применение промокода")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PromocodePopupTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerCampaigns",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
        steps.onCabinetDashboardPage().popupBillingBlock().closePopupIcon().click();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").balanceWidgetMenu().click();
        steps.onCabinetDashboardPage().balanceWidgetButton("Ввести промокод").click();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Поп-ап «Использование промокода». Скриншот")
    public void shouldSeePromcodePopup() {
        steps.onCabinetDashboardPage().promocodePopup().waitUntil(isDisplayed())
                .should(hasText("Использование промокода\nВведите промокод на получение услуг и воспользуйтесь ими " +
                        "в списке объявлений.\nВведите промокод\nАктивировать"));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Не существующий промокод")
    public void shouldSeeNotFoundErrorMessage() {
        mockRule.with("cabinet/ApiServiceAutotruPromocodeUserUnknownPost").update();

        steps.onCabinetDashboardPage().promocodePopup().inputPromocode("qrqererqreq");
        steps.onCabinetDashboardPage().promocodePopup().activate().click();
        steps.onCabinetDashboardPage().promocodePopup().error().should(hasText("Промокод не найден"));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Пустой промокод")
    public void shouSeeEmptyErrorMessage() {
        steps.onCabinetDashboardPage().promocodePopup().activate().click();
        steps.onCabinetDashboardPage().promocodePopup().error().should(hasText("Введите промокод"));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Закрытие поп-апа")
    public void shouldClosePromocodePopup() {
        steps.onCabinetDashboardPage().promocodePopup().should(isDisplayed());
        steps.onCabinetDashboardPage().promocodePopup().closePopupIcon().click();
        steps.onCabinetDashboardPage().promocodePopup().should(not(isDisplayed()));
    }
}
