package ru.auto.tests.desktop.element.cabinet.offerstat;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 19.11.18
 */

public interface OfferStat extends VertisElement {

    @Name("Блок основной информации по объявлению")
    @FindBy(".//div[contains(@class, 'Sale')]")
    VertisElement blockSaleInfo();

    @Name("Список графиков")
    @FindBy(".//div[@class = 'Island-module__island Island-module__outer-gap Island-module__inner-gap'] | " +
            ".//main/div/div[contains(@class, 'Island-module__island')] | " +
            ".//div[@class = 'Island']")
    ElementsCollection<GraphListItem> GraphicsList();


    @Step("Получаем график с индексом {i}")
    default GraphListItem getGraphic(int i) {
        return GraphicsList().should(hasSize(greaterThan(i))).get(i);
    }
}