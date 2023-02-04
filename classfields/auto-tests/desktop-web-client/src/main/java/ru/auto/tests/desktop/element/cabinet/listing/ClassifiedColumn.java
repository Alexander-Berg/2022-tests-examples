package ru.auto.tests.desktop.element.cabinet.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface ClassifiedColumn extends VertisElement, WithButton {

    @Name("Переключатель")
    @FindBy("./label[contains(@class, 'Toggle')]")
    VertisElement toggle();
}
