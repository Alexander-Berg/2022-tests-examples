package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface MorePopup extends AtlasWebElement {

    String ADD_TO_COMPARISON = "Добавить к сравнению";
    String ADD_NOTE = "Написать заметку";

    @Name("Строчка «{{ value }}»")
    @FindBy(".//div[contains(@class,'OfferCardActions__row') and contains(.,'{{ value }}')]")
    AtlasWebElement actionRow(@Param("value") String value);
}
