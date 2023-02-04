package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SpecialSales extends VertisElement {

    @Name("Список спецпредложений")
    @FindBy(".//div[contains(@class, 'IndexBlock__item ')]")
    ElementsCollection<VertisElement> itemsList();

    @Step("Получаем спецпредложение с индексом {i}")
    default VertisElement getSale(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}