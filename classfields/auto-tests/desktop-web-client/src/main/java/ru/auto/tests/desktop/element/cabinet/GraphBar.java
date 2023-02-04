package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface GraphBar extends VertisElement {

    @Name("Точка")
    @FindBy(".//div[contains(@class, '_dot_') and not(contains(@class, '_dot_hidden'))]")
    VertisElement dot();

}
