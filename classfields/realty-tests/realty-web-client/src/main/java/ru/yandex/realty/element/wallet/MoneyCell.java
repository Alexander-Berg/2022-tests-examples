package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 06.07.17.
 */
public interface MoneyCell extends AtlasWebElement {

    @Name("Ячейка денег")
    @FindBy(".//td[contains(@class, 'WalletTransactionsItem__cellAmount')]/span")
    AtlasWebElement amount();

    @Name("Ячейка типа транзакции")
    @FindBy(".//td[1]")
    AtlasWebElement type();
}
