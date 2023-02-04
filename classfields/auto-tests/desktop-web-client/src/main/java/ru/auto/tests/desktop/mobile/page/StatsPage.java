package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.catalog.Filter;

public interface StatsPage extends BasePage {

    @Name("Фильтр")
    @FindBy("//div[@class='Filters']")
    Filter filter();

    @Name("Средняя цена")
    @FindBy("//div[@class='StatsAverage']")
    VertisElement averagePrice();

    @Name("Блок «Как дешевеет это авто с возрастом»")
    @FindBy("//div[@class='StatsPricePerAge']")
    VertisElement pricePerAge();

    @Name("Элемент блока «Как дешевеет это авто с возрастом»")
    @FindBy("//div[@class='StatsPricePerAgeItem__age']/a[text()='{{ text }}']")
    VertisElement pricePerAgeItem(@Param("text") String text);

    @Name("Статистика по модификации")
    @FindBy("//div[@class='StatsModification']")
    VertisElement modificationsStats();

    @Name("Ссылка «{{ text }}» в делении по модификациям")
    @FindBy("//div[@class = 'StatsModification__row']//*[contains(@class, 'SegmentedBarGraph__segment-title') and " +
            ".= '{{ text }}']")
    VertisElement modificationUrl(@Param("text") String text);

    @Name("Среднее время продажи")
    @FindBy("//div[@class='StatsAverageTimeSale']")
    VertisElement averageTimeSale();

    @Name("Кнопка «Разместить объявление»")
    @FindBy("//a[contains(@class,'StatsAverageTimeSale__add-sale')]")
    VertisElement addSaleButton();

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'StatsNoData__content')]")
    VertisElement stub();

    @Name("Ссылка на каталог в заглушке")
    @FindBy("//div[contains(@class, 'StatsNoData__content')]//a")
    VertisElement stubCatalogUrl();
}
