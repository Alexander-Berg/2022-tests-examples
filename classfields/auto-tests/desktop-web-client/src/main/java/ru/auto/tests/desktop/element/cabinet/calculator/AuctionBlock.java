package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AuctionBlock extends VertisElement {

    @Name("Аукцион «{{ name }}»")
    @FindBy(".//div[@class='Auction__tableWrapper']//tr[@class='AuctionTableItem' and .//td[.='{{ name }}']]")
    Auction auction(@Param("name") String name);

    @Name("Список аукционов")
    @FindBy(".//div[@class='Auction__tableWrapper']//tr[@class='AuctionTableItem']")
    ElementsCollection<VertisElement> auctionsList();

    @Name("Кнопка сохранить ставки")
    @FindBy(".//div[@class='Auction__tableWrapper']//tr/td[@class='Auction__saveButton']/button")
    VertisElement saveBetsButton();

    @Name("Фильтры аукционов")
    @FindBy(".//div[@class='Auction__filters']")
    AuctionFilters auctionFilters();

}
