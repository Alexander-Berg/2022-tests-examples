package ru.auto.tests.desktop.element.group;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithRadioButton;

public interface Complectations extends VertisElement, WithRadioButton, WithCheckbox {

    @Name("Сравнение комплектаций")
    @FindBy(".//div[contains(@class, 'CardGroupCompare')]")
    ComplectationsCompare compare();

    @Name("Опции")
    @FindBy(".//div[contains(@class, 'CardGroupOptions__list')]")
    VertisElement options();
}