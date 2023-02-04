package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ExchangeGarageModal extends VertisElement {

    @Name("Авто из гаража «{{ name }}»")
    @FindBy("//div[@class = 'OfferPriceExchangeGarageModal__item'][.//div[contains(@class, '_name') and .='{{ name }}']]")
    VertisElement car(@Param("name") String name);

}
