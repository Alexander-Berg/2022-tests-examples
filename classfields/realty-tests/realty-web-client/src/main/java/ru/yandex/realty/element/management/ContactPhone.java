package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ContactPhone extends AtlasWebElement {

    @Name("Крестик «Удалить телефон»")
    @FindBy("//button[contains(@aria-label,'Удалить телефон')]/i")
    AtlasWebElement removePhone();
}
