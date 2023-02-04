package ru.yandex.realty.element.samolet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface SpecProjectDeliveryDatePopup extends AtlasWebElement {

    @Name("Элемент списка: «{{ value }}»")
    @FindBy(".//div[text()='{{ value }}']")
    AtlasWebElement item(@Param("value") String value);
}
