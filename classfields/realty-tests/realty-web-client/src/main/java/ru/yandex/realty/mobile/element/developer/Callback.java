package ru.yandex.realty.mobile.element.developer;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface Callback extends AtlasWebElement, Button {

    String CALL_ME = "Позвоните мне";

    @Name("Инпут телефона")
    @FindBy(".//input")
    AtlasWebElement phoneInput();

}
