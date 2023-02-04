package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ShowPhonePopup;
import ru.yandex.realty.element.saleads.NewListingOffer;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * Created by vicdev on 21.04.17.
 */
public interface FavoritesPage extends BasePage {

    @Name("Сообщение об отсутствии объявлений")
    @FindBy("//div[@class='FavoritesPage__stubContainer']")
    AtlasWebElement noOffersMessage();

    @Name("Список избранного")
    @FindBy("//li[contains(@class, 'FavoritesSerp__list-item')]")
    ElementsCollection<NewListingOffer> favoritesList();

    @Name("Попап «Показать телефон»")
    @FindBy("//div[contains(@class,'Modal_visible PhoneModal')]")
    ShowPhonePopup showPhonePopup();

    default NewListingOffer offer(int i) {
        return favoritesList().should(hasSize(greaterThan(i))).get(i);
    }
}
