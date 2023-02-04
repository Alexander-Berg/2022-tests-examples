package ru.auto.tests.desktop.element.cabinet.priceReport;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface OffersTable extends VertisElement {

    @Name("Иконка закрытия")
    @FindBy(".//*[contains(@class, '_close')]")
    VertisElement closeIcon();

    @Name("Список объявлений")
    @FindBy(".//div[@class = 'PriceReportWarehouse__item']")
    ElementsCollection<PriceReportOffer> offers();

}
