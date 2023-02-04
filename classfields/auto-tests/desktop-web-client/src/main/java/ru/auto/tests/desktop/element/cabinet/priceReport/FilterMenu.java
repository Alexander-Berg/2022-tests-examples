package ru.auto.tests.desktop.element.cabinet.priceReport;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FilterMenu extends VertisElement {

    String MARKET = "Рынок";

    @Name("Список дилеров")
    @FindBy(".//div[@class = 'Menu__group']//div[contains(@class, 'MenuItem')]")
    ElementsCollection<VertisElement> dealers();

    @Name("Выбранный дилер")
    @FindBy(".//div[contains(@class, 'MenuItem_checked')]")
    VertisElement checkedDealer();

    @Name("Айтем «Рынок», для сброса фильтра")
    @FindBy(".//div[contains(@class, 'MenuItem_has-clear_cross')]")
    VertisElement market();

}
