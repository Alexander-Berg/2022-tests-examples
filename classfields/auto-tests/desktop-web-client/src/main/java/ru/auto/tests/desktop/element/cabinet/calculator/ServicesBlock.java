package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 07.12.18
 */
public interface ServicesBlock extends VertisElement {

    @Name("Информация об услуге")
    @FindBy(".//div[contains(@class, 'CalculatorServiceItem__tooltip')]")
    VertisElement tooltip();

    @Name("Ввод")
    @FindBy(".//label//input")
    VertisElement input();


}
