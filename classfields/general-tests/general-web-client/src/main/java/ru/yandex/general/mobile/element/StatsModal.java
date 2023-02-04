package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface StatsModal extends VertisElement {

    @Name("Общий каунтер")
    @FindBy(".//li[contains(@class, '_statsItem')][2]//div[contains(@class, 'inline-flex')]/span[1]")
    VertisElement totalCount();

    @Name("Дата в блоке информации")
    @FindBy(".//li[contains(@class, '_statsItem')][1]/span")
    VertisElement date();

    @Name("Каунтер по сравнению с прошедшим периодом")
    @FindBy(".//li[contains(@class, '_statsItem')][2]//div[contains(@class, 'inline-flex')]/span[2]")
    VertisElement previousPeriodDifference();

    @Name("Список пустых столбцов графика")
    @FindBy(".//*[contains(@class, '_barEmpty_')]")
    ElementsCollection<VertisElement> emptyBars();

    @Name("Список не пустых столбцов графика")
    @FindBy(".//*[contains(@class, '_bar_')]")
    ElementsCollection<VertisElement> notEmptyBars();

    @Name("Список дат по оси")
    @FindBy(".//*[contains(@class, 'axis-tick')]//*[contains(@class, 'StatsBarChartText__text')]")
    ElementsCollection<VertisElement> datesAxisList();

    @Name("Список каунтеров над столбиками")
    @FindBy(".//*[contains(@class, 'label-list')]//*[contains(@class, 'StatsBarChartText__text')]")
    ElementsCollection<VertisElement> countersBarList();

}
