package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CrossLinksBlock extends VertisElement, WithButton {

    @Name("Заголовок блока")
    @FindBy(".//h3[@class = 'CrossLinks__title'] | " +
            ".//div[@class = 'cross-links__title']")
    VertisElement title();

    @Name("Рейтинг модели или марки")
    @FindBy(".//div[@class = 'CrossLinks__rating' or @class = 'cross-links__rating']")
    VertisElement rating();

    @Name("Список ссылок")
    @FindBy(".//a")
    ElementsCollection<VertisElement> linkList();

    @Step("Получаем ссылку с индексом {i}")
    default VertisElement getLink(int i) {
        return linkList().should(hasSize(greaterThan(i))).get(i);
    }
}