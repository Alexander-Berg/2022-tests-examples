package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface StatsBarChart extends VertisElement {

    @Name("Общий каунтер")
    @FindBy(".//div[contains(@class, '_totalCount')]/span[1]")
    VertisElement totalCount();

    @Name("Каунтер по сравнению с прошедшим периодом")
    @FindBy(".//div[contains(@class, '_totalCount')]/span[2]")
    VertisElement previousPeriodDifference();

    @Name("Список пустых столбцов графика")
    @FindBy(".//*[contains(@class, 'StatsBarChart__barEmpty')]")
    ElementsCollection<VertisElement> emptyBars();

    @Name("Список не пустых столбцов графика")
    @FindBy(".//*[contains(@class, 'StatsBarChart__bar_')]")
    ElementsCollection<VertisElement> notEmptyBars();

    @Name("Дата в тултипе")
    @FindBy(".//div[contains(@class, 'BarChartTooltip__date')]")
    VertisElement tooltipDate();

    @Name("Каунтер в тултипе")
    @FindBy(".//div[contains(@class, 'BarChartTooltip__value')]")
    VertisElement tooltipValue();

    @Name("Заглушка графика")
    @FindBy(".//div[contains(@class, 'StatsBarChartEmpty__chart')]")
    VertisElement emptyChart();

}
