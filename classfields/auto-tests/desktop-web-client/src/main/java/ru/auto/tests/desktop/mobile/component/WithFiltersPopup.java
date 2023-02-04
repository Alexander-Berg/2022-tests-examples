package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.mobile.element.FiltersPopup;

public interface WithFiltersPopup {

    @Name("Поп-ап выбора «{{ text }}»")
    @FindBy("//div[@class = 'FiltersPopup' and .//div[contains(@class, 'FiltersPopup__title') and .= '{{ text }}']]")
    FiltersPopup filtersPopup(@Param("text") String text);

}