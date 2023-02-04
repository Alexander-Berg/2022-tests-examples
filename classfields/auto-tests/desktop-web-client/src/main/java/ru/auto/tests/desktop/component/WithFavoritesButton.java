package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithFavoritesButton {

    @Name("Кнопка добавления в избранное")
    @FindBy(".//*[contains(@class, 'favorite')] | " +
            ".//button[contains(@class, 'ButtonFavorite')]")
    VertisElement favoriteButton();

    @Name("Кнопка удаления из избранного")
    @FindBy(".//div[contains(@class, 'ButtonFavorite_active')]")
    VertisElement favoriteDeleteButton();
}