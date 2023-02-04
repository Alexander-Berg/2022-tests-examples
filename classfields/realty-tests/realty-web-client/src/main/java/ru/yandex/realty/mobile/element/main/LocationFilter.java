package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 16.08.17.
 */
public interface LocationFilter extends AtlasWebElement {

    @Name("Поле ввода")
    @FindBy(".//input")
    AtlasWebElement input();

    @Name("Кнопка очистки")
    @FindBy(".//a")
    AtlasWebElement clearButton();
}
