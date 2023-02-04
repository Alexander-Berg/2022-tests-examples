package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface OfferWrap extends VertisElement, Image {

    @Name("Цена")
    @FindBy(".//span[contains(@class, '_price')]")
    VertisElement price();

    @Name("Название")
    @FindBy(".//span[contains(@class, '_title')]")
    VertisElement title();

    @Name("Каунтер фото")
    @FindBy(".//span[contains(@class, 'SnippetImage__counter')]")
    VertisElement photoCounter();

    @Name("Заглушка фото")
    @FindBy(".//div[contains(@class, 'SnippetImage__noPhotoContainer')]")
    VertisElement noPhoto();

}
