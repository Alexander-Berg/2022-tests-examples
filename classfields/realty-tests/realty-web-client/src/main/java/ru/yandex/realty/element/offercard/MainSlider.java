package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface MainSlider extends AtlasWebElement {

    @Name("Картинка")
    @FindBy(".//img")
    MainSlider img();
}
