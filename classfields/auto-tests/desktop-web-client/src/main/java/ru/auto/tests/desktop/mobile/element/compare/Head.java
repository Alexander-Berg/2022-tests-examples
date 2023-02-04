package ru.auto.tests.desktop.mobile.element.compare;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Head extends VertisElement {

    @Name("Поколение первой модели")
    @FindBy(".//td[@class = 'VersusHead__cell' and .//p[.= 'Поколение']]//span")
    VertisElement firstModelGeneration();

    @Name("Комплектация первой модели")
    @FindBy(".//td[@class = 'VersusHead__cell' and .//p[.= 'Комплектация']]//span")
    VertisElement firstModelComplectation();
}