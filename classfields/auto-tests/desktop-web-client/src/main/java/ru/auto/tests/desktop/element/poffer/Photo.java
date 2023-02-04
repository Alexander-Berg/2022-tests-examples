package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface Photo extends VertisElement, WithCheckbox {

    @Name("img")
    @FindBy(".//img")
    VertisElement image();
}