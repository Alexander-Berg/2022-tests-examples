package ru.auto.tests.desktop.element.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Textbook extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'TutorialPromo__title')]")
    VertisElement title();

    @Name("Список статей")
    @FindBy(".//a[contains(@class, 'TutorialPromo__item')]")
    ElementsCollection<VertisElement> articlesList();

    @Name("Ссылка «Все статьи»")
    @FindBy(".//a[contains(@class, 'SpoilerLink_type_crossblock')] | " +
            ".//a[contains(@class, 'SpoilerLink_type_default')]")
    VertisElement allArticlesUrl();

    @Step("Получаем статью с индексом {i}")
    default VertisElement getArticle(Integer i) {
        return articlesList().should(hasSize(greaterThan(i))).get(i);
    }
}