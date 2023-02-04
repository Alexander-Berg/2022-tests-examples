package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface FavoritesPopupItem extends VertisElement, WithButton {

    @Name("Ссылка на оффер")
    @FindBy(".//a[contains(@class, 'nav-top-favorite-popup__item-link')] | " +
            ".//a[contains(@class, 'HeaderFavoritesPopupItem-module__link')] | " +
            ".//a[contains(@class, 'HeaderFavoritesPopupItem__link')]")
    VertisElement link();

    @Name("Заметка")
    @FindBy(".//*[contains(@class, 'sale-note')] | " +
            "..//div[contains(@class, 'OfferNoteView')]")
    VertisElement note();

    @Name("Кнопка удаления из избранного")
    @FindBy(".//div[contains(@class, 'ButtonFavorite_active')]")
    VertisElement deleteButton();

    @Name("Кнопка добавления в сравнение")
    @FindBy(".//div[contains(@class, 'ButtonCompare')]")
    VertisElement addToCompareButton();

    @Name("Иконка изменения цены")
    @FindBy(".//*[contains(@class, 'ListingItemPriceHistory__discountIcon')]")
    VertisElement priceHistoryIcon();

    @Name("Статистика звонков")
    @FindBy(".//div[contains(@class, 'callsBadge')]")
    VertisElement callsStats();
}