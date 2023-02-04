package ru.yandex.realty.element.developer;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.mobile.element.Link;

public interface Slide extends AtlasWebElement, Link {

    String CHOOSE_FLAT = "Выбрать квартиру";

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//a[contains(@class, 'Button')][contains(., '{{ value }}')]")
    AtlasWebElement button(@Param("value") String value);

}
