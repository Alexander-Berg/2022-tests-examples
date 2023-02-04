package ru.auto.tests.desktop.element.group;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCallbackPopup;
import ru.auto.tests.desktop.component.WithTradeIn;
import ru.auto.tests.desktop.element.card.PricePopup;

public interface GroupOffer extends VertisElement, WithTradeIn,
        WithCallbackPopup {

    @Name("Комплектация")
    @FindBy(".//a[contains(@class, 'CardGroupListingItem__titleLink')]")
    VertisElement complectation();

    @Name("Характеристики")
    @FindBy(".//div[contains(@class, 'CardGroupListingItem__techSummary')]")
    VertisElement info();

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'PriceNewOffer__price')]")
    VertisElement price();

    @Name("Иконка добавления в избранное")
    @FindBy(".//div[contains(@class, 'ButtonFavorite_size_l')]")
    VertisElement addToFavoritesIcon();

    @Name("Иконка удаления из избранного")
    @FindBy(".//div[contains(@class, 'ButtonFavorite_active')]")
    VertisElement deleteFromFavoritesIcon();

    @Name("Ссылка «Подробнее о предложении»")
    @FindBy(".//a[contains(@class, 'CardGroupListingItem__innerLink')]")
    VertisElement additionalInfoUrl();

    @Name("Кнопка «Показать контакты»")
    @FindBy(".//button[contains(@class, 'CardGroupListingItemPhoneButton')]")
    VertisElement showContactsButton();

    @Name("Поп-ап с ценами")
    @FindBy("//div[contains(@class, 'PriceNewOffer__popup')]")
    PricePopup pricePopup();

}
