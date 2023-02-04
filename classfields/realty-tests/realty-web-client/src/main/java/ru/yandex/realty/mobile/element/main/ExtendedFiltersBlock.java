package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface ExtendedFiltersBlock extends FiltersBlock {

    @Name("Селектор «{{ filterName }}»")
    @FindBy(".//select[./../button//span/text() = '{{ filterName }}']")
    AtlasWebElement selector(@Param("filterName") String filterName);

    @Name("Искать в описании объявления")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_includeTag')]")
    AtlasWebElement includeTags();

    @Name("Исключить, если в описании")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_excludeTag')]")
    AtlasWebElement excludeTags();
}
