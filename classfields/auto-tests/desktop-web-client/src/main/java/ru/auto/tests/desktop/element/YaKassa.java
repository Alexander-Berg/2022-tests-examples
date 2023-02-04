package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithSelect;

public interface YaKassa extends VertisElement, WithSelect {

    @Name("Поле ввода «Номер карты»")
    @FindBy("//input[@placeholder = 'Номер карты']")
    VertisElement cardNumberInput();

    @Name("Поле ввода «ММ»")
    @FindBy("//input[@placeholder = 'ММ']")
    VertisElement monthInput();

    @Name("Поле ввода «ГГ»")
    @FindBy("//input[@placeholder = 'ГГ']")
    VertisElement yearInput();

    @Name("Поле ввода «CVC»")
    @FindBy("//input[@placeholder = 'CVC']")
    VertisElement cvcInput();

    @Name("Кнопка «Заплатить»")
    @FindBy("//button[contains(@class, 'yoomoney-checkout-button')]")
    VertisElement payButton();
}
