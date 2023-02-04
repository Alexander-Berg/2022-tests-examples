package ru.auto.tests.desktop.element.cabinet.wallet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.cabinet.Calendar;

public interface WalletHeader extends VertisElement {

    @Name("Кнопка открытия календаря")
    @FindBy(".//button[contains(@class, 'DateRange__button')]")
    VertisElement calendarButton();

    @Name("Календарь")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    Calendar calendar();

    @Name("Поле ввода VIN")
    @FindBy(".//label[contains(@class, 'WalletHeader__vinInput')]//input")
    VertisElement vinInput();

    @Name("Сообщение об ошибке")
    @FindBy(".//div[contains(@class, 'vinError')]")
    VertisElement vinError();

    @Name("Иконка очистки поля ввода VIN")
    @FindBy(".//i[contains(@class, 'TextInput__clear_visible')]")
    VertisElement clearIcon();
}
