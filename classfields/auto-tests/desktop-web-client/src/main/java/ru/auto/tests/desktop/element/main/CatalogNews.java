package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CatalogNews extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'Index__title-link')]")
    VertisElement title();

    @Name("Список новинок каталога")
    @FindBy(".//div[@class = 'IndexCatalog__item']")
    ElementsCollection<VertisElement> modelsList();

    @Name("Ссылка «Перейти в каталог»")
    @FindBy(".//a[contains(@class, 'Index__all-link')]")
    VertisElement goToCatalogUrl();

    @Step("Получаем модель с индексом {i}")
    default VertisElement getModel(Integer i) {
        return modelsList().should(hasSize(greaterThan(i))).get(i);
    }
}
