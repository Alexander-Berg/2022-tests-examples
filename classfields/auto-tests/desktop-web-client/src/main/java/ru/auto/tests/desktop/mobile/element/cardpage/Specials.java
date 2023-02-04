package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Specials extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'IndexBlock__title')]")
    VertisElement title();

    @Name("Список спецпредложений")
    @FindBy(".//div[contains(@class, 'IndexBlock__item ')]")
    ElementsCollection<VertisElement> specialsList();

    @Step("Получаем спецпредложение с индексом {i}")
    default VertisElement getSpecial(int i) {
        return specialsList().should(hasSize(greaterThan(i))).get(i);
    }
}