package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FavCard extends Link {

    String EMAIL_AND_PUSH_TEXT = "Почта, push-уведомления";
    String EMAIL_TEXT = "Только эл. почта";
    String PUSH_TEXT = "Только push-уведомления";
    String OFF = "Выключены";

    @Name("Тип оповещений")
    @FindBy(".//label[contains(@class, 'SelectButton')]/button")
    VertisElement notificationType();

    @Name("Тайтл")
    @FindBy(".//span[contains(@class, '__title')]")
    VertisElement title();

    @Name("Сабтайтл")
    @FindBy(".//span[contains(@class, '__subtitle')]")
    VertisElement subtitle();

    @Name("Фильтры")
    @FindBy(".//span[contains(@class, 'FavoritesSearchCard__filters')]")
    VertisElement filters();

    @Name("Удалить")
    @FindBy(".//button[contains(@class,'__deleteButton')]")
    VertisElement deleteButton();

    @Name("Кнопка избранного")
    @FindBy(".//button[contains(@class,'FavoritesCard__favoriteButton')]")
    VertisElement favButton();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//a[contains(@class,'FavoritesCard__phoneButton')]")
    VertisElement phoneShow();

    @Name("Кнопка открытия чата")
    @FindBy(".//div[contains(@class, 'ChatButton')]//button")
    VertisElement chatButton();

    @Name("Аватар")
    @FindBy(".//div[contains(@class, 'description')]//img")
    VertisElement avatar();

    @Name("Заглушка фото")
    @FindBy(".//img[contains(@class, 'SnippetImagePlaceholder')]")
    VertisElement dummyImg();

    @Name("Заглушки фото сохраненного поиска")
    @FindBy(".//img[contains(@class, 'PlaceholderImage__smallImage')]")
    ElementsCollection<VertisElement> dummyImgs();

    @Name("Заметка")
    @FindBy(".//div[contains(@class, '_note_')]")
    VertisElement notice();

    default String getUrl() {
        return link().getAttribute("href");
    }

}
