package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Statistics extends VertisElement {

    @Name("Столбики статистики")
    @FindBy(".//div[contains(@class, 'BarChart__chart')]")
    VertisElement bars();

    @Name("Столбики статистики без данных - серые")
    @FindBy(".//div[contains(@class, 'BarChart__gray')]")
    ElementsCollection<VertisElement> noDataBars();

    @Name("Столбики статистики с данными, кроме сегодняшнего - светлоголубые")
    @FindBy(".//div[contains(@class, 'BarChart__lightBlue')]")
    ElementsCollection<VertisElement> dataBarsExceptToday();

    @Name("Столбики статистики с данными, сегодняшний, - синий")
    @FindBy(".//div[contains(@class, 'BarChart__blue')]")
    ElementsCollection<VertisElement> dataBarsToday();

    @Name("Кол-во просмотров")
    @FindBy(".//div[contains(@class, 'Statistics__stat_')][1]")
    VertisElement viewsCount();

    @Name("Кол-во контактов")
    @FindBy(".//div[contains(@class, 'Statistics__stat_')][2]")
    VertisElement contactsCount();

    @Name("Кол-во добавлений в избранное")
    @FindBy(".//div[contains(@class, 'Statistics__stat_')][3]")
    VertisElement favoritesAddedCount();

}
