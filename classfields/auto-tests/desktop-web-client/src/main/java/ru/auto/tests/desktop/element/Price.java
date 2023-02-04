package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Price extends VertisElement {

    @Name("Иконка сниженной цены")
    @FindBy(".//*[contains(@class, 'IconSvg_price-down')]")
    VertisElement priceDownIcon();
}