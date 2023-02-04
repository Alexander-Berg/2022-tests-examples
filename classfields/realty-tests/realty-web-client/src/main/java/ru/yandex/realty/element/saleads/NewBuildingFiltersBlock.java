package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface NewBuildingFiltersBlock extends RealtyElement, DropDownButton, InputField, SelectButton, SelectionBlock, Link {

    @Name("Поле «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_price-with-type')]")
    InputField price();

    @Name("Саджест {{ value }}")
    @FindBy(".//ul[contains(@class, 'Suggest__list')]//li[contains(., '{{ value }}')]")
    AtlasWebElement suggest(@Param("value") String value);

    @Name("Саджест ")
    @FindBy(".//ul[contains(@class, 'Suggest__list')]//li")
    ElementsCollection<AtlasWebElement> suggest();

    @Name("Площадь")
    @FindBy(".//fieldset[contains(@class, 'FormFieldSet_name_area')]")
    FieldSetBlock area();

    @Name("Доп кнопки в строке адреса")
    @FindBy("//div[@class='FiltersFormField__refinements-selector']")
    Link geoButtons();

    @Name("Кнопка выбора расширенных фильтров")
    @FindBy(".//div[contains(@class, 'FiltersFormField_section_extra')]//span")
    AtlasWebElement showMoreFiltersButton();

    @Name("Кнопка «Найти»")
    @FindBy(".//div[contains(@class, 'FormField_name_submit')]//button")
    AtlasWebElement submitButton();

    @Name("Прыщь")
    @FindBy(".//span[contains(@class, 'FiltersFormField__badges-label')]")
    AtlasWebElement badge();

    @Name("Бэйджик «{{ value }}»")
    @FindBy("//div[contains(@class,'Popup_visible')]//div[contains(@class, 'GeoBadges__item')]" +
            "[contains(., '{{ value }}')]")
    Badge badges(@Param("value") String value);

    @Name("Сбросить всё")
    @FindBy(".//span[contains(.,'Сбросить всё')]")
    AtlasWebElement cancelAll();

}
