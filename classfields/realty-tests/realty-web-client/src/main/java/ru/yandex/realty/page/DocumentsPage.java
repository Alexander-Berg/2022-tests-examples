package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectionBlock;

public interface DocumentsPage extends BasePage, SelectionBlock, Link, InputField, Button {

    String DOCUMENTS_DOWNLOAD = "Скачать шаблоны";
    String SEND_DOCUMENTS = "Отправить";
    String ENTER_YOUR_EMAIL = "Введите свою электронную почту";

    @Name("Успешная отправка на почту")
    @FindBy(".//div[contains(@class,'DocumentsDownload__send-message')]")
    AtlasWebElement message();

    @Name("Вопросик возле чекбокса {{ value }}")
    @FindBy(".//label[contains(.,'{{ value }}')]//a")
    AtlasWebElement labelLink(@Param("value") String value);

    @Name("Карусель документов")
    @FindBy(".//a[contains(@class,'Documents__list-item')]")
    ElementsCollection<AtlasWebElement> docCarousel();
}
