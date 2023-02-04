package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.dealers.listing.MapPopup;

public interface DealerMapPage extends BasePage {

    @Name("Карта")
    @FindBy("//div[contains(@class, 'page-dealers-listing__container-map')]")
    VertisElement map();

    @Name("Точка на карте")
    @FindBy("//*[contains(@class, '_circleDotIcon')]")
    VertisElement mapPoint();

    @Name("Поп-ап дилера на карте")
    @FindBy("//div[contains(@class, 'dealer-map__balloon-content_visible')]")
    MapPopup mapPopup();
}
