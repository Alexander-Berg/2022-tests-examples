package ru.auto.tests.cabinet.orders;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CREDIT;
import static ru.auto.tests.desktop.consts.Pages.ORDERS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Заявки на кредит под менеджером")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CreditManagerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/Manager",
                "cabinet/ApiAccessClientManager",
                "cabinet/CommonCustomerGetManager",
                "cabinet/ProductsApplicationCreditTariffs",
                "cabinet/ProductsApplicationCreditTariffTurnOff",
                "cabinet/ProductsApplicationCreditTariffTurnOn",
                "cabinet/ProductsCreateCarsNew",
                "cabinet/ProductsCreateCarsUsed",
                "cabinet/ProductsCreditConfigurationCarsNew",
                "cabinet/ProductsCreditConfigurationCarsUsed").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(ORDERS).path(CREDIT).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Страница кредитов под менеджером")
    public void shouldSeeCreditBlock() {
        steps.onCabinetOrdersCreditPage().creditBlock().should(hasText("Заявки на кредит\nКредитная форма — " +
                "новый канал привлечения клиентов, позволяющий увеличить прибыль салона путем продаж автомобилей " +
                "в кредит.\nНастроить уведомления о кредитных заявках можно на странице рассылок.\nНовые автомобили\n" +
                "Первый взнос,\n% от стоимости автомобиля\n0%\n10%\n15%\n20%\n25%\nДругой %\n" +
                "Годовая ставка по кредиту\nот\nCрок кредита\nот 9 лет\nдо 14 лет\nСохранить изменения\n" +
                "Тариф подключения\nФикcированная оплата\nСледующий платёж 4 января 2021 г.\n" +
                "Отключить 15 000 ₽ / 7 дней\nОплата за заявки\nНе забудьте отключить тариф\n" +
                "Отключить 350 ₽ / заявка\nАвтомобили с пробегом\n" +
                "Первый взнос,\n% от стоимости автомобиля\n0%\n10%\n15%\n20%\n25%\nГодовая ставка по кредиту\nот\n" +
                "Cрок кредита\nот 1 годa\nдо 15 лет\nСохранить изменения\nТариф подключения\nФикcированная оплата\n" +
                "Следующий платёж 5 января 2021 г.\nОтключить 16 000 ₽ / 7 дней\nОплата за заявки\n" +
                "Подключить 360 ₽ / заявка"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Вкл/Выкл фиксированный платеж для новых автомобилей")
    public void shouldClickFixedPaymentNew() {
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .pressedToggle("Отключить 15").click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .notPressedToggle("Подключить 15").waitUntil(isDisplayed()).click();
        steps.onCabinetOrdersCreditPage().confirmPopup().waitUntil(isDisplayed());
        steps.onCabinetOrdersCreditPage().buttonContains("Подключить тариф").click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .pressedToggle("Отключить 15").waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Вкл/Выкл фиксированный платеж для автомобилей с пробегом")
    public void shouldClickFixedPaymentUsed() {
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .pressedToggle("Отключить 16").waitUntil(isDisplayed()).click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .notPressedToggle("Подключить 16").waitUntil(isDisplayed()).click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .pressedDisabledToggle("Отключить 16").waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Вкл/Выкл единоразовый платеж для новых автомобилей")
    public void shouldClickSinglePaymentNew() {
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .pressedToggle("Отключить 350").click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .notPressedToggle("Подключить 350").waitUntil(isDisplayed()).click();
        steps.onCabinetOrdersCreditPage().confirmPopup().waitUntil(isDisplayed());
        steps.onCabinetOrdersCreditPage().buttonContains("Подключить тариф").click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .pressedToggle("Отключить 350").waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Вкл/Выкл единоразовый платеж для автомобилей с пробегом")
    public void shouldClickSinglePaymentUsed() {
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .notPressedToggle("Подключить 360").waitUntil(isDisplayed()).click();
        steps.onCabinetOrdersCreditPage().confirmPopup().waitUntil(isDisplayed());
        steps.onCabinetOrdersCreditPage().buttonContains("Подключить тариф").click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .pressedToggle("Отключить 360").waitUntil(isDisplayed()).click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Тариф изменён"));
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .notPressedToggle("Подключить 360").waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Изменение первого взноса, годовой ставки, срока кредита для новых автомобилей")
    public void shouldChangeCreditConfigurationNew() {
        mockRule.with("cabinet/ProductsCreditConfigurationCarsNewPut").update();

        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .button("25%").click();
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .input("от", "30%");
        steps.dragAndDrop(steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .periodSlider(), 10, 0);
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Новые автомобили")
                .button("Сохранить изменения").click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Конфигурация тарифа изменена"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Изменение первого взноса, годовой ставки, срока кредита для автомобилей с пробегом")
    public void shouldChangeCreditConfigurationUsed() {
        mockRule.with("cabinet/ProductsCreditConfigurationCarsUsedPut").update();

        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .button("25%").click();
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .input("от", "30%");
        steps.dragAndDrop(steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .periodSlider(), 10, 0);
        steps.onCabinetOrdersCreditPage().creditBlock().creditSection("Автомобили с пробегом")
                .button("Сохранить изменения").click();
        steps.onCabinetOrdersCreditPage().notifier().should(isDisplayed())
                .should(hasText("Конфигурация тарифа изменена"));
    }
}
