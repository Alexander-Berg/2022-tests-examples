package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.main.GeoSelectPopup;

public interface BasePage extends WebPage, WithButton, WithNotifier {

    @Name("Поп-ап")
    @FindBy("//div[contains(@class, 'Popup_visible')] | " +
            "//div[contains(@class, 'Modal_visible')] | " +
            "//div[contains(@class, 'popup_visible')]")
    Popup popup();

    @Name("Поп-ап регионов")
    @FindBy("//div[contains(@class, 'Popup_visible')][.//div[contains(@class, 'GeoSelectPopup')]]")
    GeoSelectPopup geoSelectPopup();

}
