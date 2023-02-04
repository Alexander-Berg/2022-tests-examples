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

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Применение промокода")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class ActivatePromocodeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/ApiServiceAutotruPromocodeUserPost",
                "cabinet/ApiServiceAutotruPromocode").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Активация промокода")
    public void shouldActivatePromocode() {
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
        steps.onCabinetDashboardPage().popupBillingBlock().closePopupIcon().click();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").balanceWidgetMenu().click();
        steps.onCabinetDashboardPage().balanceWidgetButton("Ввести промокод").click();
        steps.onCabinetDashboardPage().promocodePopup().inputPromocode("testtest11");
        steps.onCabinetDashboardPage().promocodePopup().activate().click();
        steps.onCabinetDashboardPage().promocodePopup().waitUntil(hasText("Промокод активирован\n" +
                "По этому промокоду вы можете бесплатно воспользоваться услугами:\n«Поднятие в поиске» — 1 шт\n" +
                "Внимание: если количество бесплатных услуг или срок их действия истечёт, " +
                "то они снова станут платными.\nЗакрыть"));
        steps.onCabinetDashboardPage().promocodePopup().close().click();
        steps.onCabinetDashboardPage().promocodePopup().waitUntil(not(isDisplayed()));
    }
}
