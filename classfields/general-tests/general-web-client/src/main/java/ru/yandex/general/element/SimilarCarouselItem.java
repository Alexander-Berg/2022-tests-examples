package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SimilarCarouselItem extends VertisElement, Link, Image {

    @Name("Цена")
    @FindBy(".//div[contains(@class, '_subTextBold')]")
    VertisElement price();

    @Name("Название")
    @FindBy(".//div[contains(@class, '_title')]")
    VertisElement title();

    @Name("Заглушка фото")
    @FindBy(".//img[contains(@class, 'SnippetImagePlaceholder')]")
    VertisElement dummyImg();

    @Name("Кнопка добавления в избранное")
    @FindBy(".//button[contains(@class, '_favoriteButton')]")
    VertisElement favorite();

    @Name("Иконка «18+»")
    @FindBy(".//*[contains(@class, '_ageIcon_')]")
    VertisElement adultAgeIcon();

}
