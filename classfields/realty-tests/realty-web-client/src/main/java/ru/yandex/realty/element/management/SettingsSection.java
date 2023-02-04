package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.RealtyElement;

public interface SettingsSection extends AtlasWebElement {

    @Name("Поле ввода")
    @FindBy(".//input")
    RealtyElement input();

    @Name("Крестик очищения")
    @FindBy(".//span[contains(@class,'TextInput__clear_visible')]")
    AtlasWebElement clearSign();

    @Name("Ошибка «{{ value }}»")
    @FindBy(".//div[contains(@class, 'form-message__visible')]//div[contains(@class,'Notification__critical') " +
            "and contains(.,'{{ value }}')]")
    AtlasWebElement error(@Param("value") String value);
}
