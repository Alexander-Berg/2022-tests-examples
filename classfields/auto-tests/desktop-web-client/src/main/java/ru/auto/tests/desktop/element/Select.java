package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface Select extends VertisElement, WithInput {

    @Name("Кнопка открытия селекта")
    @FindBy(".//button[contains(@class, 'Select__button')] |" +
            ".//button[contains(@class, 'select__button')] | " +
            ".//label[contains(@class, 'Select__button')]")
    VertisElement selectButton();

}
