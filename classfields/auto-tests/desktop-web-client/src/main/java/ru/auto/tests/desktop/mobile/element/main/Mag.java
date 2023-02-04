package ru.auto.tests.desktop.mobile.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Mag extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'IndexJournal__logo')] | " +
            ".//a[contains(@class, 'CardGroupJournal__logo')]")
    VertisElement title();

    @Name("Список статей")
    @FindBy(".//div[@class = 'IndexBlock__item']")
    ElementsCollection<VertisElement> articlesList();

    @Step("Получаем статью с индексом {i}")
    default VertisElement getArticle(int i) {
        return articlesList().should(hasSize(greaterThan(i))).get(i);
    }
}
