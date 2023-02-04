package ru.yandex.arenda.element.lk.houseservice;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Input;

public interface MetersInput extends Input {

    @Name("Ошибка «{{ value }}»")
    @FindBy(".//span[contains(@class, 'InputDescription__description') and contains(.,'{{ value }}')]")
    AtlasWebElement errorDescription(@Param("value") String value);
}
