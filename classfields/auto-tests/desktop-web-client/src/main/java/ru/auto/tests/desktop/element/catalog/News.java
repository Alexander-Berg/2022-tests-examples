package ru.auto.tests.desktop.element.catalog;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface News extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//h2")
    VertisElement title();

    @Name("Список моделей")
    @FindBy(".//div[contains(@class, 'tile_gallery-loaded')]")
    ElementsCollection<NewsItem> modelsList();

    @Step("Получаем модель с индексом {i}")
    default NewsItem getModel(int i) {
        return modelsList().should(hasSize(greaterThan(i))).get(i);
    }
}
