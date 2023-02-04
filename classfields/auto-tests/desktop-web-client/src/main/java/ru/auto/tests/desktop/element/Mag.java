package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.mag.MagArticle;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Mag extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'JournalHeader__logo')]")
    VertisElement title();

    @Name("Список статей")
    @FindBy(".//div[@class = 'Journal__item'] | " +
            ".//a[contains(@class, 'Journal__item')]")
    ElementsCollection<MagArticle> articlesList();

    @Name("Ссылка «Больше материалов»")
    @FindBy(".//a[contains(@class, 'SpoilerLink_type_crossblock')]")
    VertisElement moreArticlesUrl();

    @Step("Получаем статью с индексом {i}")
    default MagArticle getArticle(int i) {
        return articlesList().should(hasSize(greaterThan(i))).get(i);
    }
}
