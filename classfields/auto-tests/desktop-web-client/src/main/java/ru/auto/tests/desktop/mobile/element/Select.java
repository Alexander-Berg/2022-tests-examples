package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Select extends VertisElement {

    @Name("Кнопка открытия селекта")
    @FindBy(".//button[contains(@class, 'Select__button')] |" +
            ".//button[contains(@class, 'select__button')]")
    VertisElement selectButton();
}