package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.FavoritesPopup;

public interface WithFavoritesPopup {

    @Name("Поп-ап избранного")
    @FindBy("//div[contains(@class, 'nav-top-favorite-popup') and contains(@class, 'popup_visible')] | " +
            "//div[contains(@class, 'HeaderFavoritesPopup_open')]")
    FavoritesPopup favoritesPopup();
}