package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

public interface StickyTariff extends AtlasWebElement {

    String CHOOSE_TARIFF = "Перейти на тариф";
    String EXTENDED_BLOCK = "Расширенный";
    String MAXIMUM_BLOCK = "Максимальный";
    String MINIMUM_BLOCK = "Минимальный";
    String YOUR_CURRENT_TARIFF_BUTTON = "Ваш текущий тариф";


    @Name("Блок «{{ value }}»")
    @FindBy(".//div[contains(@id,'tuz_tariff')][contains(.,'{{ value }}')]")
    Button block(@Param("value") String value);
}
