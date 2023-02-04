package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ReportContentBlock extends VertisElement {

    @Name("Значение блока")
    @FindBy(".//span[contains(@class, '_value')]")
    VertisElement value();

}
