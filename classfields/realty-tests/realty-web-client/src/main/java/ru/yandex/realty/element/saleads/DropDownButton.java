package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface DropDownButton extends AtlasWebElement {

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(@class, 'Dropdown__button')][contains(., '{{ value }}')]")
    AtlasWebElement dropDownButton(@Param("value") String value);
}
