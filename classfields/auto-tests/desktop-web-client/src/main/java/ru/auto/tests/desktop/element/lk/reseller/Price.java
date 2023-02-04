package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Price extends VertisElement {

    @Name("Текст")
    @FindBy("./span[contains(@class, 'SalesItemPriceNewDesign__price')]")
    VertisElement priceText();

    @Name("Иконка редактирования")
    @FindBy("./*[contains(@class, 'SalesItemPriceNewDesign__icon')]")
    VertisElement changePriceIcon();

}
