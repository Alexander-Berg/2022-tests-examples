package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.GeoPopup;
import ru.auto.tests.desktop.mobile.element.GeoSuggestPopup;

public interface WithGeoPopup {

    @Name("Поп-ап выбора региона")
    @FindBy("//div[contains(@class, 'FiltersPopup') and .//div[contains(@class, 'GeoSelectPopup')]]")
    GeoPopup geoPopup();

    @Name("Поп-ап поиска региона")
    @FindBy("//div[contains(@class, 'FiltersSuggestPopup')]")
    GeoSuggestPopup geoSuggestPopup();
}