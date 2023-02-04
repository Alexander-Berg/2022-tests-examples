package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FavCard extends Link, Button {

    String WRITE = "Написать";

    @Name("Кнопка избранного")
    @FindBy(".//button[contains(@class,'FavoritesOfferCard__favoriteButton')]")
    VertisElement favButton();

    @Name("Показать телефон")
    @FindBy(".//a[contains(@class, 'phoneCallLink')]")
    VertisElement phoneShow();

    @Name("Тип оповещений")
    @FindBy(".//button[contains(@class, '__settingsButton')]")
    VertisElement notificationType();

    @Name("Тайтл")
    @FindBy(".//span[contains(@class, '__title')]")
    VertisElement title();

    @Name("Сабтайтл")
    @FindBy(".//span[contains(@class, '__subtitle')]")
    VertisElement subtitle();

    @Name("Удалить")
    @FindBy(".//button[contains(@class,'__deleteButton')]")
    VertisElement deleteButton();

    @Name("Заглушка фото")
    @FindBy(".//img[contains(@class, 'SnippetImagePlaceholder')]")
    VertisElement dummyImg();

    @Name("Заглушка фото сохраненного поиска")
    @FindBy(".//img[contains(@class, 'PlaceholderImage__smallImage')]")
    VertisElement searchDummyImg();

    default String getUrl() {
        return link().getAttribute("href");
    }

}
