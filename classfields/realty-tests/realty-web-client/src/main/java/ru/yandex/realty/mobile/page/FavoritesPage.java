package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.mobile.element.listing.FavoritesSnippet;
import ru.yandex.realty.mobile.element.listing.HintPopup;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FavoritesPage extends BasePage {

    @Name("Список офферов")
    @FindBy("//li[contains(@class,'SerpListItem_type_offer')]")
    ElementsCollection<FavoritesSnippet> offersList();

    @Name("Попап с информацией")
    @FindBy("//div[contains(@class, 'HintPopup')]")
    HintPopup hintPopup();

    default FavoritesSnippet offer(int n) {
        return  offersList().waitUntil(hasSize(greaterThan(n))).get(n);
    }

}
