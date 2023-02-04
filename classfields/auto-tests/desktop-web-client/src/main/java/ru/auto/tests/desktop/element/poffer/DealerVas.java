package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DealerVas extends VertisElement {

    @Name("Услуги")
    @FindBy(".//div[contains(@class, 'VasFormDealer__items')]")
    VertisElement vases();

    @Name("Услуга «{{ text }}»")
    @FindBy(".//label[contains(@class, 'VasFormDealerItem ') and .//div[.= '{{ text }}']]")
    VertisElement vas(@Param("text") String text);
}