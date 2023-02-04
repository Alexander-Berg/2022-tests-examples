package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Tab;
import ru.yandex.general.mobile.element.FavCard;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FavoritesPage extends BasePage {

    String PROFILES = "Профили";
    String SEARCHES = "Поиски";

    @Name("Карточки избранного")
    @FindBy("//div[@role = 'listItem']")
    ElementsCollection<FavCard> favoritesCards();

    @Name("Кнопка удаления всех")
    @FindBy(".//span[contains(@class,'PersonalOffersControls')]//button[contains(@class,'PersonalOffersControls__btn')]")
    VertisElement deleteAll();

    @Name("Чекбокс выбора всех")
    @FindBy(".//label[contains(@class,'FavoritesTabBar') and contains(@class, 'Checkbox')]")
    VertisElement checkAll();

    @Name("Первый сниппет")
    @FindBy("//div[@role = 'listItem'][contains(@style, 'top: 0px; left: 0px;')]")
    FavCard firstCard();

    @Name("Таб «{{ value }}»")
    @FindBy("//label[contains(@class, 'FavoritesTabBar')][contains(., '{{ value }}')]")
    Tab tab(@Param("value") String value);

    default FavCard firstFavCard() {
        favoritesCards().should(hasSize(greaterThan(0)));
        return firstCard();
    }

}
