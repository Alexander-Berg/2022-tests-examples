package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BillinpPopupPaymentMethod extends VertisElement {

    @Name("Название")
    @FindBy(".//div[contains(@class, 'BillingPaymentMethods__tileText')]")
    VertisElement title();
}