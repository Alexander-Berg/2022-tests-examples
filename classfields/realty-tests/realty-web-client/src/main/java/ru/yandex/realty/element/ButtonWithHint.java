package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ButtonWithHint extends AtlasWebElement {

    @Name("Подсказка")
    @FindBy(".//span[contains(@class,'HoverHint__icon')]")
    RealtyElement hint();
}
