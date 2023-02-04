package ru.yandex.arenda.element.lk.outstaff;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Button;

public interface OutstaffFlatFilters extends Button {

    String SEARCH_BUTTON = "Найти";

    @Name("Адрес, ID")
    @FindBy(".//input[@id='filters-address']")
    AtlasWebElement addressFilter();
}
