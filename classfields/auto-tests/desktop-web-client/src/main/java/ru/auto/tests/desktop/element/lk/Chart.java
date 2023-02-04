package ru.auto.tests.desktop.element.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Chart extends VertisElement {

    @Name("Список столбиков на графике")
    @FindBy(".//div[contains(@class, 'SalesChart__item')]")
    ElementsCollection<VertisElement> itemsList();

    @Step("Получаем столбик с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}