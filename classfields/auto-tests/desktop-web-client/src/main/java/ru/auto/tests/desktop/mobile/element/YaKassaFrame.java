package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface YaKassaFrame extends VertisElement {

    @Name("Номер карты")
    @FindBy(".//input[@placeholder = 'Номер карты']")
    VertisElement cardNumber();

    @Name("Активна до «Месяц»")
    @FindBy(".//input[@placeholder = 'ММ']")
    VertisElement month();

    @Name("Активна до «Год»")
    @FindBy(".//input[@placeholder = 'ГГ']")
    VertisElement year();

    @Name("CVC")
    @FindBy(".//input[@placeholder = 'CVC']")
    VertisElement cardCvc();

    @Name("Электронная почта")
    @FindBy(".//input[@name = 'cps_email']")
    VertisElement email();

    @Name("Кнопка «Заплатить»")
    @FindBy("//button[contains(@class, 'yoomoney-checkout-button')]")
    VertisElement pay();
}
