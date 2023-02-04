package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface Modal extends AtlasWebElement, Link, Button {

    @Name("Закрытие модалки")
    @FindBy(".//button[@class = 'CloseModalButton ']")
    AtlasWebElement close();

}
