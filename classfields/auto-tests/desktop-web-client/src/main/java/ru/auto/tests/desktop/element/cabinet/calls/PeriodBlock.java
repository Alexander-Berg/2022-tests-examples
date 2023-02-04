package ru.auto.tests.desktop.element.cabinet.calls;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 30.05.18
 */
public interface PeriodBlock extends VertisElement {

    @Name("Период с")
    @FindBy(".//span[contains(@class, 'input_period_from')]")
    VertisElement periodFrom();

    @Name("Период по")
    @FindBy(".//span[contains(@class, 'input_period_to')]")
    VertisElement periodTo();
}
