package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

public interface DevModal extends AtlasWebElement, Button {

    String CANCEL = "Отменить";

    @Name("Телефон застройщика «{{ value }}»")
    @FindBy(".//div[contains(@class, 'developer')][./h3[contains(., '{{ value }}')]]//span[contains(@class, 'phoneNumber')]")
    AtlasWebElement devPhone(@Param("value") String value);

}
