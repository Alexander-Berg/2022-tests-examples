package ru.auto.tests.desktop.element.cabinet.priceReport;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PriceReportTableRow extends VertisElement {

    @Name("Марка/модель/поколение")
    @FindBy(".//div[contains(@class, 'tableCell_markModel')]")
    VertisElement markModel();

    @Name("Комплектация")
    @FindBy(".//div[contains(@class, 'tableCell_complectation')]")
    VertisElement complectation();

    @Name("Год")
    @FindBy(".//div[contains(@class, 'tableCell_year')]")
    VertisElement year();

    @Name("Столбец «Ваш склад»")
    @FindBy(".//div[contains(@class, 'tableCol_warehouse')]")
    PricesColumn warehouseColumn();

    @Name("Столбец конкурентов")
    @FindBy(".//div[contains(@class, 'tableCol_filter')]")
    PricesColumn competitorColumn();

    @Name("Таблица со списком объявлений")
    @FindBy(".//div[@class = 'PriceReportWarehouse']")
    OffersTable offersTable();

}
