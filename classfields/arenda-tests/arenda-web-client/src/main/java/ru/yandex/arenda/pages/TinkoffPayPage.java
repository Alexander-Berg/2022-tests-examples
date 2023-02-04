package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface TinkoffPayPage extends BasePage {

    @Name("Номер карты")
    @FindBy(".//input[@id = 'pan']")
    AtlasWebElement cardNumber();

    @Name("Номер сvc")
    @FindBy(".//input[@id = 'card_cvc']")
    AtlasWebElement cvc();

    @Name("Дата истечения карты")
    @FindBy(".//input[@id = 'expDate']")
    AtlasWebElement expDate();
}
