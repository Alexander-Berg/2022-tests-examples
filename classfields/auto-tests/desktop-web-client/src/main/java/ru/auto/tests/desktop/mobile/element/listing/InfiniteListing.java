package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.SaleListItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface InfiniteListing extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//p[contains(@class, 'ListingInfiniteMobile__title')]")
    VertisElement title();

    @Name("Список объявлений")
    @FindBy(".//div[contains(@class, 'ListingInfiniteMobile__listing')]//div[@class = 'ListingItemRegular'] | " +
            ".//div[contains(@class, 'ListingInfiniteMobile__listing')]//div[@class = 'ListingItemBig']")
    ElementsCollection<SaleListItem> salesList();

    @Step("Получаем объявление с индексом {i}")
    default SaleListItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }
}
