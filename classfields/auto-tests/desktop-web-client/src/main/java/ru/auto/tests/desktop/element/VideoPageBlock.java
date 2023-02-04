package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VideoPageBlock extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//h2")
    VertisElement title();

    @Name("Список объектов")
    @FindBy(".//a[contains(@class,'VideoJournalSection__item')] | " +
            ".//div[contains(@class, 'VideoYoutubeSection__item')] | " +
            ".//a[@class = 'VideoRelatedArticlesSection__articleTitle']")
    ElementsCollection<VertisElement> itemsList();

    @Step("Получаем объект с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}