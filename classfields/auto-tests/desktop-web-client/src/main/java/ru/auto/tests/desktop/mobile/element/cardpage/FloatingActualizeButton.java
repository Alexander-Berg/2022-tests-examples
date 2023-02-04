package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface FloatingActualizeButton extends VertisElement, WithButton {

    @Name("Иконка помощи")
    @FindBy(".//span[contains(@class, 'ButtonActualizeMobile__icon')]")
    VertisElement helpIcon();
}