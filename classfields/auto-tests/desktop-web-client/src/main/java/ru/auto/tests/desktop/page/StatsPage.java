package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithMMMFilter;

public interface StatsPage extends BasePage, WithMMMFilter, WithButton {

    @Name("Средняя цена")
    @FindBy("//div[@class='StatsAverage']")
    VertisElement averagePrice();

    @Name("Кнопка «Оценить мой авто»")
    @FindBy("//div[@class='StatsAverage__evaluation']/a")
    VertisElement evaluationButton();

    @Name("Как дешевеет это авто с возрастом")
    @FindBy("//div[@class = 'StatsPricePerAge']")
    VertisElement pricePerAge();

    @FindBy("//div[@class = 'StatsModification']")
    VertisElement modificationsStats();

    @FindBy("//div[@class = 'StatsAverageTimeSale']")
    VertisElement averageTimeSale();

    @Name("Кнопка «Разместить объявление»")
    @FindBy("//a[contains(@class,'StatsAverageTimeSale__add-sale')]")
    VertisElement addSaleButton();

    @Name("Точка на графике")
    @FindBy("//*[contains(@class,'PricePerAgeGraph__bullet_clickable')][1]")
    VertisElement graphDot();

    @Name("Ссылка «{{ text }}» в делении по модификациям")
    @FindBy("//div[@class = 'StatsModification__row']//*[contains(@class, 'SegmentedBarGraph__segment-title') and " +
            ".= '{{ text }}']")
    VertisElement modificationUrl(@Param("text") String text);

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'StatsNoData__content')]")
    VertisElement stub();
}
