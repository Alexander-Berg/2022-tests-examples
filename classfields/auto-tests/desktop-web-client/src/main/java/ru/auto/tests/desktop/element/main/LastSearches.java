package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LastSearches extends VertisElement {

    @Name("Список поисков")
    @FindBy(".//div[@class = 'IndexSearchHistory__item'] | " +
            ".//div[@class = 'IndexSearchHistoryItem']")
    ElementsCollection<LastSearchesItem> searchesList();

    @Name("Кнопка «Вперед»")
    @FindBy(".//div[contains(@class, 'NavigationButton_next')]")
    VertisElement nextButton();

    @Name("Кнопка «Назад»")
    @FindBy(".//div[contains(@class, 'NavigationButton_prev')]")
    VertisElement previousButton();

    @Step("Получаем поиск с индексом {i}")
    default LastSearchesItem getSearch(int i) {
        return searchesList().should(hasSize(greaterThan(i))).get(i);
    }
}