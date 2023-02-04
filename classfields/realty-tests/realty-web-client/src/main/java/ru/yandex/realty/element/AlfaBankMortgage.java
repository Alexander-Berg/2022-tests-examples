package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface AlfaBankMortgage {

    String GET_DISCOUNT = "Получить скидку";

    @Name("Модуль заявки на ипотеку альфабанка")
    @FindBy(".//div[contains(@class,'AlfaBankMortgage__container')]")
    Button alfaBankMortgageContainer();
}
