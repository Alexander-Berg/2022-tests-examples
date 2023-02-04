package ru.auto.tests.desktop.element.cabinet.agency;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 01.10.18
 */
public interface StoItem extends VertisElement {

    @Name("Приоритет")
    @FindBy(".//button")
    VertisElement priority();
}
