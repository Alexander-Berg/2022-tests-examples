package ru.yandex.realty.element.touch;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface PopupWithLi extends AtlasWebElement {

    @Name("Элемент списка: «{{ value }}»")
    @FindBy(".//li[contains(.,'{{ value }}')]")
    AtlasWebElement item(@Param("value") String value);
}
