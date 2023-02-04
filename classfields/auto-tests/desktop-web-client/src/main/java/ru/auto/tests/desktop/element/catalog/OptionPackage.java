package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface OptionPackage extends VertisElement {

    @Name("Чекбокс пакета опций")
    @FindBy(".//label//span")
    VertisElement checkbox();
}