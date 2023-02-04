package ru.auto.tests.desktop.element.cabinet.wallet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Graph extends VertisElement {

    @Name("Список столбиков на графике")
    @FindBy(".//*[@class = 'recharts-layer recharts-bar-rectangle']")
    ElementsCollection<VertisElement> barList();

    @Name("Тултип")
    @FindBy(".//div[contains(@class, 'recharts-tooltip-wrapper')]")
    VertisElement tooltip();

    @Step("Получаем столбик с индексом {i}")
    default VertisElement getBar(int i) {
        return barList().should(hasSize(greaterThan(i))).get(i);
    }
}