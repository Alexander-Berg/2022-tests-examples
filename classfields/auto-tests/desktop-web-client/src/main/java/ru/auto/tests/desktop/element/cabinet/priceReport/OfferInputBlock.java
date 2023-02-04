package ru.auto.tests.desktop.element.cabinet.priceReport;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface OfferInputBlock extends VertisElement, WithInput {

    @Name("Статичное значение")
    @FindBy(".//div[contains(@class, '_inputStaticValue')]")
    VertisElement staticValue();

}
