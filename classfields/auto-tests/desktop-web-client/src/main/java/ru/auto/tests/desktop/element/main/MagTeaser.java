package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface MagTeaser extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'JournalTeaser__headingLink')]")
    VertisElement title();

    @Name("Список статей")
    @FindBy(".//li[contains(@class, 'JournalTeaserListOfArticles__article_visible')]")
    ElementsCollection<MagTeaserArticle> articlesList();

    @Name("Кнопка «Все материалы»")
    @FindBy(".//a[contains(@class, 'Button_color_transparentBlackBlue')]")
    VertisElement allArticlesButton();

    @Step("Получаем статью с индексом {i}")
    default MagTeaserArticle getArticle(int i) {
        return articlesList().should(hasSize(greaterThan(i))).get(i);
    }
}
