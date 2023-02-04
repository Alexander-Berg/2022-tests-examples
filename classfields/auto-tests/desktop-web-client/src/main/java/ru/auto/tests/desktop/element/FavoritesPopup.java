package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FavoritesPopup extends VertisElement {

    @Name("Список избранного")
    @FindBy(".//*[contains(@class, 'item nav-top')] | " +
            ".//div[contains(@class, 'TopNavigationFavoritesPopupItem-module__container')] | " +
            ".//div[contains(@class, 'HeaderFavoritesPopupItem-module__container')] | " +
            ".//div[contains(@class, 'HeaderFavoritesPopupItem__grid')]")
    ElementsCollection<FavoritesPopupItem> favoritesList();

    @Step("Получаем избранное с индексом {i}")
    default FavoritesPopupItem getFavorite(int i) {
        return favoritesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Заглушка")
    @FindBy(".//div[contains(@class, 'popup-placeholder_type_favorites')] |" +
            ".//div[@class = 'HeaderFavoritesPopup__empty']")
    VertisElement stub();

    @Name("Кнопка «Войти»")
    @FindBy(".//a[contains(@class, 'button_action_login')] |" +
            ".//a[contains(@class, 'Button_color_blue')]/span/span")
    VertisElement loginButton();

    @Name("Блок удаления неактивных офферов")
    @FindBy(".//div[@class = 'FavoritesDeleteNotActiveOffers']")
    DeleteFavoritesNotActiveOffers deleteNotActiveOffers();

}
