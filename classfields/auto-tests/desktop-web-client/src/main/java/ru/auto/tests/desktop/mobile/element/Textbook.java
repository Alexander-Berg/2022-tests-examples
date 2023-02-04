package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Textbook extends VertisElement {

    @Name("Список статей")
    @FindBy(".//div[@class = 'IndexBlock__item']")
    ElementsCollection<VertisElement> articlesList();

    @Name("Ссылка «Все»")
    @FindBy(".//a[contains(@class, 'tutorial-links__all')] | " +
            ".//a[contains(@class, 'Button')]")
    VertisElement allArticlesUrl();

    @Step("Получаем статью с индексом {i}")
    default VertisElement getArticle(Integer i) {
        return articlesList().should(hasSize(greaterThan(i))).get(i);
    }
}
