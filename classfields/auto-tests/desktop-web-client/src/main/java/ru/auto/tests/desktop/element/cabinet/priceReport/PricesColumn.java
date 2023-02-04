package ru.auto.tests.desktop.element.cabinet.priceReport;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PricesColumn extends VertisElement {

    @Name("Наличие")
    @FindBy(".//div[contains(@class, 'availability')]/*")
    VertisElement availability();

    @Name("Мин. цена со скидкой")
    @FindBy(".//span[@class = 'PriceReportTableRow__minPriceDiscount' or " +
            "@class = 'PriceReportTableRow__minPriceDiscount HoveredTooltip__trigger']")
    VertisElement minPriceDiscount();

    @Name("Мин. цена")
    @FindBy(".//span[@class = 'PriceReportTableRow__minPrice' or " +
            "@class = 'PriceReportTableRow__minPrice HoveredTooltip__trigger']")
    VertisElement minPrice();

    @Name("Макс. цена")
    @FindBy(".//span[@class = 'PriceReportTableRow__maxPrice' or " +
            "@class = 'PriceReportTableRow__maxPrice HoveredTooltip__trigger']")
    VertisElement maxPrice();

}
