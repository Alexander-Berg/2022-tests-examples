package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.InputField;

public interface PriceOfAgentOffer extends InputField {

    @Name("Кнопка редактирования цены")
    @FindBy(".//button[contains(@class,'editable-price__edit-button')]")
    AtlasWebElement editButton();

    @Name("Кнопка редактирования цены")
    @FindBy(".//button[contains(@class,'editable-price__submit-button')]")
    AtlasWebElement submitButton();

    @Name("Значение цены")
    @FindBy(".//span[contains(@class,'editable-price__price')]")
    AtlasWebElement value();
}
