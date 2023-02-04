package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ComplectationBlock extends Block {

    @Name("Выбранные опции")
    @FindBy(".//div[@class = 'options-checkboxes__selected-options-list']")
    Options selectedOptions();
}