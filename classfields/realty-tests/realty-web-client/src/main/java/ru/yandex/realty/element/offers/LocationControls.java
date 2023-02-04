package ru.yandex.realty.element.offers;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.saleads.InputField;

/**
 * Created by vicdev on 08.06.17.
 */
public interface LocationControls extends InputField {

    @Name("Иконка «Очистить»")
    @FindBy(".//span[contains(@class,'TextInput__clear_visible')]")
    RealtyElement iconClear();

    @Name("Строчка с адресом {{ value }}")
    @FindBy(".//ul/li[@class='Suggest__list-item']/div/div[text()='{{ value }}']")
    RealtyElement suggestListItem(@Param("value") String value);

    default AtlasWebElement suggest() {
        return input("Укажите адрес");
    }

    default AtlasWebElement flat() {
        return input("Квартира");
    }
}
