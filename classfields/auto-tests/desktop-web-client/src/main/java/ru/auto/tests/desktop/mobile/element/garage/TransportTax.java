package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface TransportTax extends VertisElement {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//div[contains(@class, 'GarageCardTaxSelect ') and contains(., '{{ text }}')]")
    VertisElement button(@Param("text") String text);

    @Name("Айтем «{{ text }}» в попапе")
    @FindBy("//div[contains(@class, 'GarageCardTax__modal')]" +
            "//div[@class = 'GarageCardTaxList__itemText' and .= '{{ text }}']")
    VertisElement itemInPopup(@Param("text") String text);

}
