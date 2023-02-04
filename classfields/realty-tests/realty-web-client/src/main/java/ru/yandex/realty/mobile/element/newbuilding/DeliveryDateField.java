package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface DeliveryDateField extends AtlasWebElement {

    @Name("Значение поля")
    @FindBy(".//div[@class='FiltersFormFieldOpener__label']")
    AtlasWebElement label();
}
