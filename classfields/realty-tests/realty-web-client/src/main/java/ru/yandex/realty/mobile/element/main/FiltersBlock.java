package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.ButtonWithText;
import ru.yandex.realty.mobile.element.SelectWithText;

public interface FiltersBlock extends Button, SelectWithText {

    String FIELD_GARAGE_TYPE = "Тип гаража";
    String FIELD_HOUSE_TYPE = "Тип дома";
    String FIELD_COMMERCIAL_TYPE = "Тип коммерческой недвижимости";

    @Name("Фильтр количества комнат «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_roomsTotal')]")
    Button room();

    @Name("Фильтр новой/вторички")
    @FindBy(".//div[contains(@class, 'FormField_name_newFlat')]")
    Button newFlat();

    @Name("Фильтр цены")
    @FindBy(".//fieldset[contains(@class, 'FormFieldSet_name_price')]")
    FieldBlock price();

    @Name("Фильтр региона")
    @FindBy(".//fieldset[contains(@class, 'FormFieldSet_name_geo-rgid')]")
    FieldBlock region();

    @Name("ЖК/метро/район")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_geo-refinements')]")
    FieldBlock metroAndStreet();

    @Name("Блок «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_')][.//*[.='{{ value }}']]")
    FieldBlock byName(@Param("value") String value);

    @Name("Попап ")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    FilterPopup filterPopup();

    @Name("Кнопка «Показать»")
    @FindBy(".//fieldset[contains(@class,'FormFieldSet_name_actions')]//button[not(contains(.,'Показать')) and contains(@class,'Button_view_yellow')]")
    AtlasWebElement applyFiltersButton();

    @Name("Поле ввода минимальной цены")
    @FindBy(".//input[@id = 'filters_control_price_from']")
    AtlasWebElement priceMin();

    @Name("Поле ввода максимальной цены")
    @FindBy(".//input[@id = 'filters_control_price_to']")
    AtlasWebElement priceMax();

    @Name("Поле ввода минимальной площади")
    @FindBy(".//input[@id = 'filters_control_area_from']")
    AtlasWebElement areaMin();

    @Name("Поле ввода максимальной площади")
    @FindBy(".//input[@id = 'filters_control_area_to']")
    AtlasWebElement areaMax();

    @Name("Поле ввода минимальной площади")
    @FindBy(".//input[@id = 'filters_control_lotArea_from']")
    AtlasWebElement areaLotMin();

    @Name("Поле ввода максимальной площади")
    @FindBy(".//input[@id = 'filters_control_lotArea_to']")
    AtlasWebElement areaLotMax();

}
