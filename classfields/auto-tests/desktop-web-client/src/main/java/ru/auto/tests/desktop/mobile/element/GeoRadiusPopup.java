package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface GeoRadiusPopup extends VertisElement {

    @Name("Гео-радиус «{{ text }}»")
    @FindBy(".//div[.= '{{ text }}']")
    VertisElement geoRadius(@Param("text") String text);
}