package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface InputClearCross extends AtlasWebElement {

    @Name("Крестик очистки")
    @FindBy("..//*[contains(@class,'Input__close')]")
    ArendaElement clearInputCross();
}
