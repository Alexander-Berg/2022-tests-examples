package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.wallet.AddCardForm;
import ru.yandex.realty.element.wallet.Balance;
import ru.yandex.realty.element.wallet.Cards;
import ru.yandex.realty.element.wallet.CardsPopup;
import ru.yandex.realty.element.wallet.PromocodePaymentPopup;
import ru.yandex.realty.element.wallet.TransactionHistory;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface WalletPage extends WebPage, Button {

    @Name("Блок «Баланс кошелька»")
    @FindBy("//div[contains(@class, 'WalletSection__container') and contains(.,'Баланс кошелька')]")
    Balance balance();

    @Name("Блок с картами")
    @FindBy("//div[contains(@class, 'WalletSection__container') and contains(.,'Добавить')]")
    Cards cards();

    @Name("Блок «История платежей»")
    @FindBy("//div[contains(@class, 'WalletSection__container') and contains(.,'платеж')]")
    TransactionHistory transactionHistory();

    @Name("Попап для оплаты")
    @FindBy("//div[@class='Portal']//div[contains(@class,'BasePaymentModal__modal')]")
    CardsPopup cardsPopup();

    @Name("Форма для ввода данных с карты (iframe)")
    @FindBy("//iframe[contains(@class, 'KassaCardForm__iframe')]")
    AtlasWebElement addCardFormIFrame();

    @Name("Форма для ввода данных с карты")
    @FindBy(".//form[@class = 'yoomoney-checkout-cardpayment__payment-information']")
    AddCardForm addCardForm();

    @Name("Подтверждение удаления карты")
    @FindBy("//div[contains(@class, 'Modal_visible') and contains(.,'Вы уверены')]//button[contains(.,'Да')]")
    AtlasWebElement confirmDeleteCardButton();

    @Name("Сообщение с ошибкой о неправильном номере карты")
    @FindBy("//div[contains(@class, 'yoomoney-checkout-tooltip-content')]")
    ElementsCollection<AtlasWebElement> errors();

    @Name("Попап оплаты промокодом")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    PromocodePaymentPopup promocodePopup();

    @Name("Саджест карт")
    @FindBy(".//div[contains(@class,'Popup_visible')]//div[contains(@class,'Menu__item')]")
    ElementsCollection<AtlasWebElement> cardList();
}
