package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.cabinet.priceReport.FilterMenu;
import ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportTableRow;
import ru.auto.tests.desktop.element.header.SubHeader;

public interface CabinetPriceReportPage extends BasePage, SubHeader {

    String ORDINARY_OFFERS = "Обычные объявления";
    String CARS_THAT_COMPETITORS_DONT_HAVE = "Автомобили, которых нет у ваших конкурентов";
    String CARS_THAT_YOU_DONT_HAVE = "Автомобили которых нет у вас, но есть у конкурентов";
    String CARS_THAT_MAY_NOT_HAVE_DISCOUNTS = "Авто с объявлениями, в которых может не быть скидок";
    String THIS_ONE = "вот этот";

    @Name("Список рядов в таблице")
    @FindBy("//div[contains(@class, 'PriceReport__tableRow')][.//div[contains(@class, '_info')]]")
    ElementsCollection<PriceReportTableRow> tableRows();

    @Name("Ячейка информации")
    @FindBy("//div[contains(@class, 'PriceReport__tableInfoCell')]")
    VertisElement infoCell();

    @Name("Попап цены")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[@class = 'PriceReportTooltipContent']")
    Popup pricePopup();

    @Name("Тултип по ховеру")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'HoveredTooltip')]")
    VertisElement hoveredTooltip();

    @Name("Кнопка открытия фильтра по дилеру")
    @FindBy("//div[@class = 'PriceReportMarketFilter']//button")
    VertisElement dealerFilterButton();

    @Name("Попап выбора дилера")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'PriceReportMarketFilter__menu')]")
    FilterMenu dealerFilterPopup();

}
