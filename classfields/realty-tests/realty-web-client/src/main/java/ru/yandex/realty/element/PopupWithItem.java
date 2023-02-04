package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface PopupWithItem extends AtlasWebElement {

    @Name("Элемент списка с чекбоксами и лейблами в попапе")
    @FindBy(".//label[contains(., '{{ value }}')]")
    AtlasWebElement label(@Param("value") String value);

    @Name("Элемент списка: «{{ value }}»")
    @FindBy(".//div[contains(@class, 'Menu__item') and contains(., '{{ value }}')]")
    AtlasWebElement item(@Param("value") String value);

    @Name("Элемент для ввода: «{{ value }}»")
    @FindBy(".//input[contains(@name, '{{ value }}')]")
    AtlasWebElement input(@Param("value") String value);
}
