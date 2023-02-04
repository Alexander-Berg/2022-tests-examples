package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectButton;
import ru.yandex.realty.element.saleads.SelectionBlock;

public interface ExtendedFiltersBlock extends SelectionBlock, SelectButton, Link {

    @Name("Фильтр этажей")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_floors')]")
    InputField floorFilter();

    @Name("Фильтр площади кухни")
    @FindBy(".//div[contains(@class, 'FormField_name_kitchenSpace')]")
    InputField areaKitchenFilter();

    @Name("Дополнительные параметры")
    @FindBy(".//div[contains(@class,'CardFiltersShowMoreFormField')]")
    AtlasWebElement showMoreParams();
}
