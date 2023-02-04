package ru.auto.tests.cabinet.dashboard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.page.YandexTrustPage;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.desktop.consts.Owners.CHERNOVPA;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 07.12.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Дашборд.Пополнение баланса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class AvaliablityButtonsPaymentTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

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
                "cabinet/DealerCampaigns",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/ApiClientInvoicePersons").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Активность кнопки. Все поля заполнены")
    public void shouldButtonBillActiveFull() {
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт")
                .should(not(isEnabled()));
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys(valueOf(getRandomShortInt()));
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт").
                should(not(isEnabled()));
        steps.onCabinetDashboardPage().popupBillingBlock().choicePayer().click();
        steps.onCabinetDashboardPage().selectPayer("теставтору теставтору теставтору").click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт")
                .should(not(isEnabled()));
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Оплата картой")
                .should(not(isEnabled()));
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт")
                .should(isEnabled());
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Оплата картой")
                .should(isEnabled());

    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Ссылка на оферту")
    public void shouldClickAgreementUrl() {
        String uri = steps.onCabinetDashboardPage().popupBillingBlock().linkOferta().getAttribute("href");
        steps.moveCursor(steps.onCabinetDashboardPage().popupBillingBlock().linkOferta());
        steps.onCabinetDashboardPage().popupBillingBlock().linkOferta().click();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.switchToNextTab();
        assertThat(urlSteps.getCurrentUrl()).isEqualTo(uri);
    }

    @Test
    @Category({Regression.class})
    @Owner(CHERNOVPA)
    @DisplayName("Ссылка на Бух. Документы")
    public void shouldClickAccountingDocumentsUrl() {
        steps.onCabinetDashboardPage().popupBillingBlock().closePopupIcon().click();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").balanceWidgetMenu().click();
        steps.onCabinetDashboardPage().balanceWidgetButton("Бухгалтерские документы").click();
        urlSteps.switchToNextTab();
        urlSteps.shouldUrl(startsWith("https://passport.yandex.ru/"), 2);
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Закрытие поп-апа оплаты")
    public void shouldCloseBillPopup() {
        steps.onCabinetDashboardPage().popupBillingBlock().closePopupIcon().click();
        steps.onCabinetDashboardPage().popupBillingBlock().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Оплата картой более 50000р")
    public void shouldCardBillOver50000Rub() {
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys("50001");
        steps.onCabinetDashboardPage().popupBillingBlock().choicePayer().click();
        steps.onCabinetDashboardPage().selectPayer("теставтору теставтору теставтору").click();
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonCardPay().click();
        steps.onCabinetDashboardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Оплата картой возможна для сумм до 50000 рублей"));
    }

    @Step("Вводим данные карты")
    private void inputCardData() {
        steps.driver().switchTo().frame(YandexTrustPage.CARD_FRAME_ID);

        steps.onYandexTrustPage().cardNumber().click();
        steps.onYandexTrustPage().cardNumber().sendKeys(YandexTrustPage.CARD_NUMBER);
        steps.onYandexTrustPage().cardValidDate().click();
        steps.onYandexTrustPage().cardValidDate().sendKeys(YandexTrustPage.CARD_DATE);
        steps.onYandexTrustPage().cvc().click();
        steps.onYandexTrustPage().cvc().sendKeys(YandexTrustPage.CVC);

        steps.driver().switchTo().defaultContent();
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Успешное выставление счета")
    public void shouldSeeBill() {
        mockRule.with("cabinet/ApiClientInvoicePost").update();

        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys(valueOf(getRandomShortInt()));
        steps.onCabinetDashboardPage().popupBillingBlock().choicePayer().click();
        steps.onCabinetDashboardPage().selectPayer("теставтору теставтору теставтору").click();
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Файл со счётом будет загружен на ваш компьютер"));
        steps.onCabinetDashboardPage().popupBillingBlock().should(not(isDisplayed()));
    }
}
