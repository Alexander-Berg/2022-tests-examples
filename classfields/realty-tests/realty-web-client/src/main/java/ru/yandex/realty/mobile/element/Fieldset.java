package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.saleads.InputField;

public interface Fieldset extends AtlasWebElement {

    @Name("Поле «{{ value }}»")
    @FindBy(".//div[contains(@class,'FiltersFormField_name')][.//label[.='{{ value }}']]")
    InputField field(@Param("value") String value);
}
