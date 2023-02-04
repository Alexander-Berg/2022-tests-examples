package ru.auto.tests.desktop.element.cabinet.priceReport;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PriceReportOffer extends VertisElement {

    String WITH_DISCOUNT = "Со скидкой";
    String WITHOUT_DISCOUNT = "Без скидки";
    String MAXIMUM = "Максимальная";
    String TRADE_IN = "Trade-In";
    String CREDIT = "Кредит";
    String KASKO = "КАСКО";

    @Name("ID авто")
    @FindBy(".//div[@class = 'PriceReportWarehouse__vin']")
    VertisElement id();

    @Name("Инпут блок «{{ text }}»")
    @FindBy(".//div[contains(@class, '_inputBlock')][.//div[contains(@class, '_inputName') and .= '{{ text }}']]")
    OfferInputBlock inputBlock(@Param("text") String text);

}
