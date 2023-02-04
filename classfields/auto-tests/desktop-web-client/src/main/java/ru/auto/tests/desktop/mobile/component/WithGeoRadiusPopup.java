package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.GeoRadiusPopup;

public interface WithGeoRadiusPopup {

    @Name("Поп-ап «Расширить радиус поиска»")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    GeoRadiusPopup geoRadiusPopup();
}