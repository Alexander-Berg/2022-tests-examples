package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.RealtyElement;

/**
 * Created by kopitsa on 16.08.17.
 */
public interface UniversalFilter extends AtlasWebElement {

    @Name("Поле ввода «от»")
    @FindBy(".//input[contains(@id, 'Min')]")
    AtlasWebElement inputMin();

    @Name("Поле ввода «до»")
    @FindBy(".//input[contains(@id, 'Max')]")
    AtlasWebElement inputMax();

    @Name("Тумблер «{{ value }}»")
    @FindBy(".//span[./label/text() = '{{ value }}']//div[contains(@class, 'tumbler__sticker_position_right')]")
    RealtyElement tumbler(@Param("value") String value);

    @Name("Чекбокс «{{ value }}»")
    @FindBy(".//span[contains(@class, 'checkbox')]//label[contains(., '{{ value }}')]")
    RealtyElement checkbox(@Param("value") String value);
}
