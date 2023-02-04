package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SelectWithText extends AtlasWebElement {

    @Name("Селектор «{{ filterName }}»")
    @FindBy(".//select[contains(., '{{ filterName }}')]")
    AtlasWebElement selector(@Param("filterName") String filterName);

}
