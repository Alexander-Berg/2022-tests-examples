package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;

public interface FilterButton extends VertisElement, WithCheckbox {

    @Name("Кнопка сброса")
    @FindBy(".//div[contains(@class, 'PseudoInput__clear')] | " +
            ".//*[contains(@class, 'TagIconClear')]")
    VertisElement resetButton();
}