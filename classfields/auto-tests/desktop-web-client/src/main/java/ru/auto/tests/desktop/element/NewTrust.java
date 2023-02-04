package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.history.ReportBundle;

public interface NewTrust extends VertisElement, WithButton {

    String VIP_PACKAGE = "Пакет VIP";
    String TURBO_SALE = "Турбо продажа";
    String CUSTOM_PACKAGE = "Наборный пакет";
    String FRESH_SERVICE = "Поднятие в поиске";
    String HIGHLIGHT_SERVICE = "Выделение объявления цветом";
    String REMEMBER_CARD = "Запомнить карту для следующих заказов";
    String PAYMENT_SUCCESS = "Платёж совершён";

    @Name("Поле ввода «Номер карты»")
    @FindBy("//input[@id = 'card_number-input']")
    VertisElement cardNumberInput();

    @Name("Поле ввода «ММ»")
    @FindBy("//input[@name = 'expiration_month']")
    VertisElement monthInput();

    @Name("Поле ввода «ГГ»")
    @FindBy("//input[@name = 'expiration_year']")
    VertisElement yearInput();

    @Name("Поле ввода «CVC»")
    @FindBy("//input[@name = 'cvn']")
    VertisElement cvcInput();

    @Name("Кнопка «Заплатить»")
    @FindBy("//button[contains(., 'Оплатить')]")
    VertisElement payButton();

    @Name("Тайтл статуса пополнения")
    @FindBy("//span[contains(@class, '_title')]")
    VertisElement statusTitle();

    @Name("Название услуги")
    @FindBy("//h3")
    VertisElement title();

    @Name("Описание бандла услуг")
    @FindBy("//div[contains(@class, '_bundleDescription')]")
    VertisElement bundleDescription();

    @Name("Стоимость услуги")
    @FindBy("//h2")
    VertisElement price();

    @Name("Бандл с типами отчёта")
    @FindBy("//span[contains(@class, 'BillingReportBundleList')]")
    ReportBundle reportBundleList();

    @Name("Блок автопродления")
    @FindBy("//div[@class = 'BillingRecurrentPaymentFormBlock']")
    NewTrustRecurrentBlock recurrentBlock();

    @Name("Виды оплаты")
    @FindBy("//div[contains(@class, 'YpcRadioGroup-Item')]")
    ElementsCollection<NewTrustPaymentMethod> paymentMethods();

}
