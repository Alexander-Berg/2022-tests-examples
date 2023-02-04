package ru.auto.tests.desktop.element.cabinet.backonsale;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Listing extends VertisElement {

    @Name("Список объявлений")
    @FindBy(".//div[(@class = 'BackOnSaleItem') or (@class = 'BackOnSaleItem BackOnSaleItem_soldOut')]")
    ElementsCollection<ListingItem> items();

    @Step("Получаем объявление {i}")
    default ListingItem getItem(int i) {
        return items().should(hasSize(greaterThan(i))).get(i);
    }
}
