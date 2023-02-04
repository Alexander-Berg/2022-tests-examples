package ru.yandex.arenda.element.lk.houseservice;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;

public interface HouseServiceMeterPopup extends Button {

    @Name("Счетчик «{{ value }}»")
    @FindBy(".//div[contains(@class, 'HouseServiceCreateCounterModalSnippet__snippet') and contains(.,'{{ value }}')]")
    AtlasWebElement meterItem(@Param("value") String value);
}
