package ru.yandex.realty.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.saleads.InputField;

public interface CommuteSuggest extends InputField {

    @Name("Элемент саджеста «{{ value }}»")
    @FindBy(".//ul/li[contains(.,'{{ value }}')]")
    AtlasWebElement suggestElement(@Param("value") String value);

    @Name("Крестик очистки саджеста")
    @FindBy("//div[@class='MapCommuteSuggest__close']")
    AtlasWebElement clearSuggest();
}
