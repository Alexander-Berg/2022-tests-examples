package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface RadioButton extends AtlasWebElement {

    @Name("Радиобатон «{{ value }}»")
    @FindBy(".//label[contains(@class, 'Radio_js_inited')][contains(., '{{ value }}')]")
    AtlasWebElement radio(@Param("value") String value);

}
