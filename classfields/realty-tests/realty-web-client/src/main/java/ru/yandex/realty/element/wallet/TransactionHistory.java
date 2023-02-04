package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 05.07.17.
 */
public interface TransactionHistory extends AtlasWebElement {

    @Name("Список транзакций")
    @FindBy(".//tr[.//td[contains(@class,'WalletTransactionsItem__cell')]]")
    ElementsCollection<MoneyCell> paymentList();
}
