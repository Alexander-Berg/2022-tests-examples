package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface Balance extends AtlasWebElement {

    @Name("Текущий баланс")
    @FindBy(".//div[contains(@class,'WalletBalance__balance')]")
    AtlasWebElement value();

    @Name("Сумма для пополнения")
    @FindBy(".//div[contains(@class,'MoneyAmountInput__container')]//input")
    AtlasWebElement input();

    @Name("Крестик для очистки поля ввода суммы")
    @FindBy(".//div[contains(@class,'MoneyAmountInput__container')]//span[contains(@class,'TextInput__clear_visible')]")
    AtlasWebElement clearButton();

    @Name("Кнопка «Пополнить»")
    @FindBy(".//button[contains(@class, 'WalletBalance__button')]")
    AtlasWebElement submitButton();

    @Name("Чекбокс «Всегда оплачивать из кошелька»")
    @FindBy(".//label[contains(@class, 'WalletBalance__preferWallet')]")
    AtlasWebElement preferWalletCheckbox();
}
