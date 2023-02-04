package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface AlfaModal {

    @Name("Попап заявки на ипотеку альфабанка")
    @FindBy(".//div[contains(@class,'AlfaBankMortgageForm__modalVisible')]//div[contains(@class,'AlfaBankMortgageForm__content')]//iframe")
    AtlasWebElement alfaPopup();
}
