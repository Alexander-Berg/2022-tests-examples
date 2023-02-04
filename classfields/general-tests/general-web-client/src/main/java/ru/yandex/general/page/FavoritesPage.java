package ru.yandex.general.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.FavCard;
import ru.yandex.general.element.Link;
import ru.yandex.general.element.Tab;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FavoritesPage extends BasePage, Link {

    String SEARCHES = "Поиски";
    String PROFILES = "Профили";

    @Name("Таб «{{ value }}»")
    @FindBy("//label[contains(@class, 'FavoritesTabBar')][contains(., '{{ value }}')]")
    Tab tab(@Param("value") String value);

    @Name("Карточки избранного")
    @FindBy(".//div[@role = 'gridItem']")
    ElementsCollection<FavCard> favoritesCards();

    @Name("Чекбокс выбора всех")
    @FindBy(".//label[contains(@class,'FavoritesTabBar')]")
    VertisElement checkAll();

    @Name("Первый сниппет")
    @FindBy("//div[@role = 'gridItem'][contains(@style, 'top: 0px; left: 0px;')]")
    FavCard firstCard();

    default FavCard firstFavCard() {
        favoritesCards().should(hasSize(greaterThan(0)));
        return firstCard();
    }

}
