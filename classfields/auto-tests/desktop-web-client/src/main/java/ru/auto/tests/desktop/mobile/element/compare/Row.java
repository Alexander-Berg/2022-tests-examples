package ru.auto.tests.desktop.mobile.element.compare;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Row extends VertisElement {

    @Name("Иконка")
    @FindBy(".//*[contains(@class, 'IconSvg')]")
    VertisElement icon();
}