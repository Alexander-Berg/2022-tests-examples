package ru.auto.tests.desktop.element.electro;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Section extends VertisElement {

    @Name("Кнопка")
    @FindBy(".//a[contains(@class, 'Button_size_xl')]")
    VertisElement button();

}
