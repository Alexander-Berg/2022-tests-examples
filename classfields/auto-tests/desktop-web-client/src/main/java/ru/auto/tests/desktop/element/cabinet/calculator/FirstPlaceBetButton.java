package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FirstPlaceBetButton extends VertisElement {

    @Name("Кнопка «Применить ставку»")
    @FindBy("./button[contains(@class, 'AuctionTableItem__firstPlaceBet_whiteHoverBlue')]")
    VertisElement button();

}
