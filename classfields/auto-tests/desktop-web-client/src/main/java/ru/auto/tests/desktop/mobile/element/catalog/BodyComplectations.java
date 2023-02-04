package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface BodyComplectations extends VertisElement {

    @Name("Список комплектаций")
    @FindBy(".//div[@class = 'catalog__packages-list-item'] | " +
            ".//div[@class = 'Complectations__item'] | " +
            ".//div[@class = 'CatalogComplectations__group']")
    ElementsCollection<VertisElement> complectationsList();

    @Step("Получаем комплектацию с индексом {i}")
    default VertisElement getComplectaion(int i) {
        return complectationsList().should(hasSize(greaterThan(i))).get(i);
    }
}
