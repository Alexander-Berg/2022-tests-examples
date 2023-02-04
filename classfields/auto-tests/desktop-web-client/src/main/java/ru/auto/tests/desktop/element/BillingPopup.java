package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.WithYaKassa;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface BillingPopup extends VertisElement, WithSelect, WithYaKassa, WithCheckbox, WithButton {

    String EXPRESS_SALE = "Экспресс продажа";
    String TURBO_SALE = "Турбо продажа";
    String PACKAGE_VIP = "Пакет VIP";
    String PAYMENT_SUCCESS = "Платёж совершён успешно";
    String BANK_CARD = "Банковская карта";
    String WALLET = "Кошелёк";
    String CONTINUE_PURCHASE = "Продолжить покупку";
    String EXIT = "Выйти";

    @Name("Заголовок»")
    @FindBy(".//div[contains(@class, 'BillingHeader__title')]")
    VertisElement header();

    @Name("Подзаголовок")
    @FindBy(".//div[contains(@class, 'BillingHeader__subtitle')]")
    VertisElement subHeader();

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'BillingHeader__price')]//span[@class = 'BillingHeader__baseCost'] | " +
            ".//div[contains(@class, 'BillingHeader__price')]")
    VertisElement priceHeader();

    @Name("Список методов оплаты")
    @FindBy(".//div[contains(@class, 'BillingPaymentMethods__tile ')]")
    ElementsCollection<BillinpPopupPaymentMethod> paymentMethodsList();

    @Name("Чекбокс «Включить автопродление»")
    @FindBy("//label[contains(@class, 'Billing__autoProlongationCheckbox')] | " +
            "//label[contains(@class, 'BillingAutoProlongation__checkbox')]")
    VertisElement autoprolongCheckbox();

    @Name("Чекбокс «Запомнить карту»")
    @FindBy("//label[contains(@class, 'BillingFooter__rememberCard')]")
    VertisElement rememberCardCheckbox();

    @Name("Кнопка «Оплатить привязанной картой»")
    @FindBy("//button[contains(@class, 'BillingFrameTiedCards__button')]")
    VertisElement tiedCardPayButton();

    @Name("Кнопка «Оплатить кошельком»")
    @FindBy("//button[contains(@class, 'BillingFrameWallet__button')]")
    VertisElement walletPayButton();

    @Name("Кнопка «Оплатить Сбербанком»")
    @FindBy("//button[contains(@class, 'BillingFrameSberbank__button')]")
    VertisElement sberbankPayButton();

    @Name("Поле ввода «Номер телефона» при оплате Сбербанком")
    @FindBy("//label[contains(@class, 'BillingFrameSberbank__phoneInput')]//input")
    VertisElement sberbankPhoneInput();

    @Name("Поле ввода «Номер телефона» при оплате Сбербанком")
    @FindBy("//label[contains(@class, 'BillingFrameSberbank__phoneInput')]//span[contains(@class, 'TextInput__error')]")
    VertisElement sberbankErrorMessage();

    @Name("Кнопка «Включить автоподнятие»")
    @FindBy("//button[contains(@class, 'BillingPaymentStatus__prolongationBoostButton')]")
    VertisElement autoFreshButton();

    @Name("Кнопка «Включить автопродление»")
    @FindBy("//div[contains(@class, 'BillingPaymentStatus__prolongation')]/button")
    VertisElement autoProlongButton();

    @Name("Сообщение «Платёж совершён успешно»")
    @FindBy("//div[contains(@class, 'BillingPaymentStatus__statusText')]")
    VertisElement successMessage();

    @Name("Переключатель")
    @FindBy(".//label[contains(@class, 'BillingVinReportSelector__tab')]")
    VertisElement vinSwitcher();

    @Name("Переключатель «{{ text }}»")
    @FindBy(".//label[contains(@class, 'BillingVinReportSelector__radio') and contains(., '{{ text }}')]")
    VertisElement vinSwitcher(@Param("text") String text);

    @Name("Переключатель «{{ text }}»")
    @FindBy(".//label[contains(@class, 'BillingVinReportSelector__radio') and contains(@class, 'Radio_checked') " +
            "and contains(., '{{ text }}')]")
    VertisElement vinSwitcherSelected(@Param("text") String text);

    @Step("Получаем метод оплаты с индексом {i}")
    default BillinpPopupPaymentMethod getPaymentMethod(int i) {
        return paymentMethodsList().should(hasSize(greaterThan(i))).get(i);
    }
}
