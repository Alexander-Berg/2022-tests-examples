package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.YandexTrustOkPopup;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 08.11.18
 */
public interface YandexTrustPage extends WebPage {

    String CARD_NUMBER = "5100007616501477";
    String CARD_DATE = "1221";
    String CVC = "900";
    String CARD_FRAME_ID = "pcidss-frame";

    @Name("Счёт")
    @FindBy("//div[@class = 'payment-details__purpose']")
    VertisElement bill();

    @Name("Сумма")
    @FindBy("//div[@class = 'payment-details__amount']")
    VertisElement amount();

    @Name("Номер карты")
    @FindBy("//span[@class = 'card_number']//input")
    VertisElement cardNumber();

    @Name("Срок действия")
    @FindBy("//span[contains(@class, 'card_valid-date')]//input")
    VertisElement cardValidDate();

    @Name("CVC")
    @FindBy("//span[contains(@class, 'card_cvc-field')]//input")
    VertisElement cvc();

    @Name("Оплатить")
    @FindBy("//button[contains(., 'Оплатить')]")
    VertisElement pay();

    @Name("Поп-ап «Платеж успешно проведен»")
    @FindBy("//div[@class = 'dialog-inner']")
    YandexTrustOkPopup okPopup();

}
