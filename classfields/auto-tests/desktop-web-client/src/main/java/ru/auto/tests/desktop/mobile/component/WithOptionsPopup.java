package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.listing.OptionsPopup;

public interface WithOptionsPopup {

    @Name("Поп-ап выбора опций")
    @FindBy("//div[@class = 'FiltersPopup' and .//div[contains(@class, 'FiltersPopup__title') and .= 'Опции']]")
    OptionsPopup optionsPopup();
}