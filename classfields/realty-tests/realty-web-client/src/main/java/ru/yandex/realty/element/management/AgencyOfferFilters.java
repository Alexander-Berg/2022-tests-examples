package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectButton;

public interface AgencyOfferFilters extends SelectButton, InputField, Link {

    String RUB_SYMBOL = "\u20BD";
    String USD_SYMBOL = "\u0024";
    String EUR_SYMBOL = "€";

    String PRICE_FROM = "от";
    String PRICE_TO = "до";

    String SEARCH_BY = "Поиск по городу и улице";

    @Name("Таб «{{ value }}»")
    @FindBy(".//li[contains(.,'{{ value }}')]")
    AtlasWebElement tab(@Param("value") String value);

    @Name("Расширенные фильтры узкого окна")
    @FindBy(".//div[@class='agency-filters__row agency-filters__row_wrap']")
    AtlasWebElement extFilters();

    @Name("Открытые фильтры услуг")
    @FindBy(".//div[contains(@class,'animate-height_state_visible')]")
    Button servicesFilters();
}