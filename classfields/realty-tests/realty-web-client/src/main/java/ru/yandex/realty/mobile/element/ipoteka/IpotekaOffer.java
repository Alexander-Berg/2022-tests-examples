package ru.yandex.realty.mobile.element.ipoteka;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface IpotekaOffer extends AtlasWebElement {

    @Name("Сыллка оффера")
    @FindBy(".//a[contains(@class,'MinifiedSerpItem__link')]")
    AtlasWebElement link();
}
