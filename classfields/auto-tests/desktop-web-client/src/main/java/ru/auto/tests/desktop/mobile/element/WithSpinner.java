package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithSpinner extends VertisElement {

    @Name("Спиннер")
    @FindBy(".//span[contains(@class, '_spinner')]")
    VertisElement spinner();

}
