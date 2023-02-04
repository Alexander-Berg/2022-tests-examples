package ru.auto.tests.desktop.element.cabinet.tradein;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface TradeInItem extends VertisElement {

    @Name("Название (марка, модель)")
    @FindBy(".//a[contains(@class, 'TradeInItem__title')]")
    VertisElement title();
}