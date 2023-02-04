package ru.auto.tests.desktop.mobile.component;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.SaleListItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface WithSalesList {

    @Name("Список объявлений")
    @FindBy(".//div[@class = 'Listing__page']/div[@class = 'ListingItemBig' or @class = 'ListingItemRegular'] | " +
            ".//div[@class = 'ListingItemBig' or @class = 'ListingItemRegular'] | "+
            ".//div[contains(@class, 'PageLike')]/div[@class = 'ListingItemRegular'] | " +
            ".//div[contains(@class, 'ListingItemSearcher')] | " +
            ".//div[@class = 'ListingAmpItemBig' or @class = 'ListingAmpItemRegular']")
    ElementsCollection<SaleListItem> salesList();

    @Step("Получаем объявление с индексом {i}")
    default SaleListItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

}
