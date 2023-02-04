package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

public interface ComplainBlock extends Button {

    @Name("Опция {{ value }}")
    @FindBy(".//div[contains(@class,'ListItem__container')][contains(.,'{{ value }}')]")
    AtlasWebElement complainOption(@Param("value") String value);

    @Name("Крестик закрытия")
    @FindBy(".//*[contains(@class,'OfferComplain__cross')]")
    AtlasWebElement closeCross();
}
