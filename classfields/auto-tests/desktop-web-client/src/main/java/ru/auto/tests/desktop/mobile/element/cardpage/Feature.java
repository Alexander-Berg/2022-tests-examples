package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface Feature extends VertisElement, WithButton {

    @Name("Тултип")
    @FindBy(".//*[contains(@class, 'IconSvg_help-filled')]")
    VertisElement tooltip();
}