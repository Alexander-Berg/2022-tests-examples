package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ServiceButton extends VertisElement {

    @Name("Счетчик")
    @FindBy(".//span[@class='SaleButtonRoyal__counter']")
    VertisElement counter();
}
