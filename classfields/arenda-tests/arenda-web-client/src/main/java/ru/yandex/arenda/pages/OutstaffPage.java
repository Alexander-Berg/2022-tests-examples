package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.lk.outstaff.OutstaffFlatFilters;
import ru.yandex.arenda.element.lk.outstaff.OutstaffFlatItem;

public interface OutstaffPage extends BasePage {

    @Name("Фильтры квартир")
    @FindBy(".//div[@data-test = 'OutstaffSearchFlats']")
    OutstaffFlatFilters outstaffFlatFilters();

    @Name("Скелетон")
    @FindBy(".//div[contains(@class,'Skeleton__item')]")
    AtlasWebElement skeletonItem();

    @Name("Список квартир")
    @FindBy(".//div[contains(@class,'OutstaffSearchFlatsItem__container')]")
    ElementsCollection<OutstaffFlatItem> outstaffFlatsItem();
}
