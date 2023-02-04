package ru.auto.tests.desktop.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface CreditFilterPopup extends VertisElement, WithInput {

    @Name("Слайдер платежа От")
    @FindBy(".//div[contains(@class, 'CreditFilterDetailsDump__row')][1]//div[contains(@class, 'Slider__toggler_from')]")
    VertisElement paymentSliderFrom();

    @Name("Слайдер платежа До")
    @FindBy(".//div[contains(@class, 'CreditFilterDetailsDump__row')][1]//div[contains(@class, 'Slider__toggler_to')]")
    VertisElement paymentSliderTo();

    @Name("Слайдер срока кредита")
    @FindBy(".//div[contains(@class, 'CreditFilterDetailsDump__row')][2]//div[contains(@class, 'Slider__toggler_to')]")
    VertisElement yearSliderTo();
}