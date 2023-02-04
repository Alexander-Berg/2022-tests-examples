package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by vicdev on 18.04.17.
 */
public interface CheckboxWithLabel extends AtlasWebElement {

    @Name("Чекбокс")
    @FindBy(".//input")
    AtlasWebElement checkbox();
}
