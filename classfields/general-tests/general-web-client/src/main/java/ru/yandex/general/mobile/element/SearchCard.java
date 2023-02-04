package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SearchCard extends Link {

    @Name("Тип оповещений")
    @FindBy(".//button[contains(@class, 'SearchCard__settingsButton')]")
    VertisElement notificationType();

    @Name("Тайтл")
    @FindBy(".//span[contains(@class, 'FavoritesSearchCard__title')]")
    VertisElement title();

    @Name("Фильтры")
    @FindBy(".//span[contains(@class, 'SearchCard__subtitle')]")
    VertisElement filters();

    @Name("Удалить")
    @FindBy(".//button[contains(@class,'FavoritesSearchCard__deleteButton')]")
    VertisElement deleteButton();

}
