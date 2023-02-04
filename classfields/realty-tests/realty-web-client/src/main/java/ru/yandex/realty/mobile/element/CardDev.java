package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;

public interface CardDev extends Button, InputField {

    @Name("Подсказка")
    @FindBy(".//span[contains(@class, 'CardDevBackCallForm__hint')]")
    AtlasWebElement hint();

    default void inputPhone(String value) {
        input().clear();
        input().sendKeys(value);
    }
}
