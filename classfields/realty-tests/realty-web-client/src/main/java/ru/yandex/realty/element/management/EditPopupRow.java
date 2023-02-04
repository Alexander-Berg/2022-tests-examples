package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

/**
 * Created by kantemirov on 06.06.17
 */
public interface EditPopupRow extends Button {

    @Name("Поле ввода")
    @FindBy(".//input")
    AtlasWebElement input();

    @Name("Линк «Добавить номер»")
    @FindBy(".//span[contains(@class, 'Link')]")
    AtlasWebElement addPhoneLink();
}
