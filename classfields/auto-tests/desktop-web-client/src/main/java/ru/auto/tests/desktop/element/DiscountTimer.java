package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DiscountTimer extends VertisElement {

    @Name("Скидка")
    @FindBy(".//div[contains(@class, 'PromoPopupDiscountTimer__buttonTitle')]")
    VertisElement discount();
}