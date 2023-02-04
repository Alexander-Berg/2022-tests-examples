package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.listing.ParamsPopup;

public interface WithParamsPopup {

    @Name("Поп-ап параметров")
    @FindBy("//div[contains(@class, 'FiltersPopup')]")
    ParamsPopup paramsPopup();

    @Name("Кнопка применения фильтров")
    @FindBy("(//div[@class = 'FiltersPopup'])[last()]//div[contains(@class, 'FiltersPopup__bottom')]")
    VertisElement applyFilters();

}
