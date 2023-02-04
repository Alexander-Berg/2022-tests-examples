package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface GeoRadiusCounters extends VertisElement {

    @Name("Гео-кольцо «{{ text }}»")
    @FindBy(".//div[@class = 'ListingGeoRadiusCounters__item' and contains(., '{{ text }}')]")
    VertisElement geoRadiusCounter(@Param("text") String Text);

    @Name("Активное гео-кольцо «{{ text }}»")
    @FindBy(".//div[@class = 'ListingGeoRadiusCounters__item ListingGeoRadiusCounters__item_active' " +
            "and contains(., '{{ text }}')]")
    VertisElement geoRadiusCounterActive(@Param("text") String Text);
}
