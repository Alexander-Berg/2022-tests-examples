package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

public interface PromocodePaymentPopup extends Button, Link {

    String FREE_USE = "Подключить бесплатно";
    String QUIT = "Выйти";

    @Name("Кнопка закрыть")
    @FindBy(".//button[contains(@class,'CloseModalButton')]")
    AtlasWebElement closeButton();

    @Name("Заголовок попапа")
    @FindBy(".//div[contains(@class,'PaymentHeader__title')]")
    AtlasWebElement popupHeader();
}
