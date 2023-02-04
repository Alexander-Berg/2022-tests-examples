package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.listing.MMMPopup;

public interface WithMmmPopup {

    @Name("Поп-ап выбора марки/модели")
    @FindBy("//div[@class = 'FiltersPopup' and (.//div[.= 'Марки'] or .//div[.= 'Выбрать модели'] " +
            "or .//div[.= 'Исключить модели'] or .//div[contains(@class, 'FilterableListGenerations')])] |" +
            "//div[@class='Portal VersusHead__portal' and (.//div[contains(@class, 'Portal__content')])]")
    MMMPopup mmmPopup();
}