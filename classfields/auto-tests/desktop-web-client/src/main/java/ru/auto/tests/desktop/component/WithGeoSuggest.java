package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.GeoSuggest;

public interface WithGeoSuggest {

    @Name("Саджест регионов")
    @FindBy("//div[contains(@class, 'RichInput__popup')] | " +
            "//div[contains(@class, 'popup_visible')]//div[contains(@class, 'suggest__result')]")
    GeoSuggest geoSuggest();
}