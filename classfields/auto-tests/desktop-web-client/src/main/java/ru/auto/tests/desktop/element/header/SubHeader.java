package ru.auto.tests.desktop.element.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface SubHeader extends VertisElement, WithButton {

    @Name("Кнопка открытия поп-апа регионов")
    @FindBy(".//div[contains(@class, 'GeoSelect__title')] | " +
            ".//div[contains(@class, 'nav-top__geo-select')]")
    VertisElement geoSelectButton();
}
