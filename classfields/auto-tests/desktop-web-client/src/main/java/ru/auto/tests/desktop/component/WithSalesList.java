package ru.auto.tests.desktop.component;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.listing.SalesListCarouselItem;
import ru.auto.tests.desktop.element.listing.SalesListItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface WithSalesList {

    @Name("Список объявлений")
    @FindBy(".//div[@class = 'ListingItemSequential'] | " +
            ".//div[@class = 'ListingItemSequential ListingItemSequential_new'] | " +
            ".//div[@class = 'ListingItem'] | " +
            ".//div[@class = 'ListingItemGroup'] | " +
            ".//div[@class = 'ListingItem ListingItem_hidden'] | " +
            ".//div[@class = 'ListingItem ListingItem_booked'] | " +
            ".//div[@class = 'ListingItem ResellerPublicListingItem']")
    ElementsCollection<SalesListItem> salesList();

    @Name("Список объявлений типа «Карусель»")
    @FindBy(".//div[@class = 'ListingItemWide'] | " +
            ".//div[@class = 'ListingItemWide ListingItemWide_pale']")
    ElementsCollection<SalesListCarouselItem> carouselSalesList();

    @Name("Список скрытых объявлений типа «Карусель»")
    @FindBy(".//div[contains(@class, 'ListingItemWide_hidden')]")
    ElementsCollection<SalesListCarouselItem> hiddenCarouselSalesList();

    @Name("Список офферов без проданных, забронированных")
    @FindBy(".//div[@class = 'ListingItem' and not(contains(., 'забронирован')) " +
            "and not(contains(., 'продан'))]")
    ElementsCollection<SalesListItem> actualSalesList();

    @Step("Получаем объявление с индексом «{i}»")
    default SalesListItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем объявление с индексом «{i}»")
    default SalesListCarouselItem getCarouselSale(int i) {
        return carouselSalesList().should(hasSize(greaterThan(i))).get(i);
    }

}
