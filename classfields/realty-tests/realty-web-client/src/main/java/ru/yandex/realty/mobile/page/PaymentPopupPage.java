package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.SelectionBlock;
import ru.yandex.realty.element.wallet.AddCardForm;

public interface PaymentPopupPage extends BasePage, SelectionBlock {

    String REMEMBER_CARD = "Запомнить карту";

    @Name("Модуль оплаты")
    @FindBy("//div[contains(@class,'PaymentModalBase__container')][contains(@class, 'Modal_visible')]")
    AtlasWebElement paymentVisible();

    @Name("Контейнер успешной оплаты")
    @FindBy("//div[contains(@class,'PaymentModalSuccess__container')]")
    AtlasWebElement successContainer();

    @Name("Крестик закрыть")
    @FindBy("//*[contains(@class,'PaymentModalBase__cross')]")
    AtlasWebElement closeCross();

    @Name("Кнопка оплаты")
    @FindBy("//div[contains(@class,'PaymentModalPaymentButton__container')]/button")
    AtlasWebElement payButton();

    @Name("Форма для ввода данных с карты (iframe)")
    @FindBy("//iframe[contains(@class, 'YandexKassaCardForm__iframe')]")
    AtlasWebElement addCardFormIFrame();

    @Name("Форма для ввода данных с карты")
    @FindBy(".//form[@class = 'yandex-checkout-cardpayment__payment-information']")
    AddCardForm addCardForm();
}
