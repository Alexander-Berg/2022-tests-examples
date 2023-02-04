package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.CloseCross;

public interface CallbackPopup extends Button, CloseCross {

    @Name("Поле ввода телефона")
    @FindBy(".//input[@id='back-call']")
    AtlasWebElement input();
}
