package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.Tab;

public interface ManagementPopup extends Button, Link, Tab {

    String SEND_CODE = "Отправить код";
    String SUBMIT_INVOICE = "Выставить счёт";
    String PAY_WITH_CARD = "Оплатить картой";
    String TO_YA_BALANCE = "Яндекс.Баланс";

    @Name("Контент контактов")
    @FindBy(".//div[@class='contacts-popup']")
    AtlasWebElement contactsContent();

    @Name("Форма добавления нового телефона")
    @FindBy(".//div[@class='phone']//input")
    AtlasWebElement phoneInput();
}
