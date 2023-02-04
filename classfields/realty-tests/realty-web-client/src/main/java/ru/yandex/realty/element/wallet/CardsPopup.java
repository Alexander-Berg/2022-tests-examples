package ru.yandex.realty.element.wallet;

import io.qameta.atlas.core.api.Retry;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.saleads.SelectionBlock;

public interface CardsPopup extends SelectionBlock, Button, Link {

    String FOLLOW_TO_PAYMENT = "Перейти к оплате";
    String USE_CARD = "Использовать карту";
    String PAY = "Заплатить";

    @Name("Кнопка «Оплатить»")
    @FindBy(".//button[contains(., 'Оплатить') or contains(., 'Подключить')]")
    AtlasWebElement paymentButton();

    @Name("Кнопка «Закрыть»")
    @FindBy(".//button[contains(@class, 'CloseModalButton')]")
    RealtyElement close();

    @Name("Методы оплаты")
    @FindBy(".//div[contains(@class,'PaymentMethod__container')]//div[contains(@class,'PaymentMethod__content')]")
    ElementsCollection<AtlasWebElement> paymentMethods();

    @Name("Платёж совершён успешно")
    @FindBy(".//div[contains(@class, 'PaymentSuccessScreen__text') and contains(.,'Платёж совершён успешно')]")
    AtlasWebElement successMessage();

    @Name("Колесико обработки платежа")
    @FindBy(".//span[contains(@class,'Spin_visible')]")
    @Retry(polling = 100, timeout = 10000)
    AtlasWebElement spinVisible();

    @Name("Окно оплаты с кошелька")
    @FindBy(".//div[contains(@class, 'WalletScreen__container')]")
    AtlasWebElement walletContainer();

    @Name("Окно оплаты с новой карты")
    @FindBy(".//div[contains(@class, 'BankCardScreen__container')]")
    AtlasWebElement cardContainer();
}
