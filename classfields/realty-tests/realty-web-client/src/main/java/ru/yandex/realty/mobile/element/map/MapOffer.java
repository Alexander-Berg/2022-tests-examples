package ru.yandex.realty.mobile.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.ButtonWithTitle;
import ru.yandex.realty.mobile.element.Link;

public interface MapOffer extends Link, ButtonWithTitle, Button {

    String CALL = "Позвонить";

    @Name("Ссылка оффера")
    @FindBy(".//a[contains(@class,'SerpItemLink')]")
    AtlasWebElement offerLink();
}
