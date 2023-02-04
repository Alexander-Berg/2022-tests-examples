package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ChartNew extends VertisElement {

    @Name("Список не пустых столбиков на графике")
    @FindBy(".//div[contains(@class, 'SalesStatsColumn')][.//div[not (contains(@class, '_value_zero'))]]")
    ElementsCollection<VertisElement> notEmptyColumnsList();

}
