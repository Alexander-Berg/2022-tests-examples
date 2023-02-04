package ru.auto.tests.desktop.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface InfiniteListing extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//p[contains(@class, 'ListingInfiniteDesktop__title')]")
    VertisElement title();

    @Name("Список объявлений")
    @FindBy(".//div[contains(@class, 'ListingInfiniteDesktop__snippet')]")
    ElementsCollection<SalesListItem> salesList();

    @Step("Получаем объявление с индексом {i}")
    default SalesListItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }
}