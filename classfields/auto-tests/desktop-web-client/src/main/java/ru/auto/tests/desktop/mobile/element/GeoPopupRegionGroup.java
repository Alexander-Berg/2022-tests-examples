package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;

public interface GeoPopupRegionGroup extends VertisElement, WithCheckbox {

    @Name("Кнопка «>»")
    @FindBy(".//div[contains(@class, 'CheckboxTreeGroup__top-button')]")
    VertisElement arrowButton();

}
