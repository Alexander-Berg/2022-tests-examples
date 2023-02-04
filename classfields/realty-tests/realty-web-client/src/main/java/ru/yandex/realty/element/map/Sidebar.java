package ru.yandex.realty.element.map;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author kantemirov
 */
public interface Sidebar extends Link {

    String SHOW_PHONE = "Показать телефон";
    String BACK_TO_FILTERS = "Назад к фильтрам";
    String TO_FAVORITE = "В избранное";
    String IN_FAVORITE = "В избранном";
    String ADD_TO_COMPARISON = "Добавить к сравнению";
    String REMOVE_FROM_COMPARISON = "Удалить из сравнения";
    String YOUR_NOTE = "Ваша заметка";
    String SEE_ONLY_YOU = "Видеть будете только вы";
    String CALL_ME = "Позвоните мне";

    @Name("Список офферов")
    @FindBy(".//li[contains(@class,'OffersSerp__list-item_type_offer')]")
    ElementsCollection<MapOffer> mapOfferList();

    @Name("Карточка новостройки")
    @FindBy(".//div[contains(@class,'MapSiteItem')]")
    MapNewBuildingCard newbuildingCard();

    @Step("Оффер коттеджного поселка")
    @FindBy(".//div[@class='VillageMapItemContainer']")
    VillageMapOffer villageMapOffer();

    @Name("Оффер избранного")
    @FindBy(".//div[contains(@class,'FavoritesMapSidebar__snippetContainer')]")
    FavoriteOffer favoriteOffer();

    @Step("Ищем оффер {offerId} в листинге")
    default MapOffer findOffer(String offerId) {
        return mapOfferList().stream()
                .filter(offer -> offer.link().getAttribute("href").contains(offerId)).findFirst().get();
    }

    default MapOffer snippetOffer(int i) {
        return mapOfferList().waitUntil(hasSize(greaterThan(i))).get(i);
    }
}
