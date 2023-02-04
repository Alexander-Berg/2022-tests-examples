package ru.yandex.realty.element.base.GeoSelectorPopup;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.saleads.InputField;

public interface MetroSuggest extends InputField {

    @Name("Элемент саджеста {{ value }}")
    @FindBy(".//div[contains(@class, '__suggestListItem') and contains(.,'{{ value }}')]")
    AtlasWebElement suggestListItem(@Param("value") String value);
}
