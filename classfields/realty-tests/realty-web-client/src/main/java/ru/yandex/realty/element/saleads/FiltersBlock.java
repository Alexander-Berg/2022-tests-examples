package ru.yandex.realty.element.saleads;

import io.qameta.atlas.core.api.Retry;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

/**
 * Created by vicdev on 17.04.17.
 */
public interface FiltersBlock extends AtlasWebElement, DropDownButton, InputField, SelectButton, SelectionBlock, Link {

    String KVARTIRU_BUTTON = "Квартиру";
    String TYPE_BUTTON = "Тип";

    String KUPIT_BUTTON = "Купить";
    String SNYAT_BUTTON = "Снять";
    String POSUTOCHO_BUTTON = "Посуточно";
    String KUPIT_DOM_BUTTON = "Купить дом";
    String NEWBUILDINGS_BUTTON = "Новостройки";

    String KOMMERCHESKUY_ITEM = "Коммерческую недвижимость";
    String DELIVERY_DATE_ITEM = "Срок сдачи";
    String YEAR_1_ITEM = "1 год";
    String SKLAD_ITEM = "Складское помещение";
    String GARAGE_ITEM = "Гараж или машиноместо";
    String ONE_HOUSE = "Отдельный дом";
    String OFIS_ITEM = "Офисное помещение";
    String DOM_ITEM = "Дом";
    String FINISHED_ITEM = "Сдан";

    String RENT_TIME = "rentTime";
    String SHORT_RENT_TIME = "SHORT";

    String ROOMS_IN_APART = "Комнат в квартире";
    String HOUSE_TYPE = "Тип дома";
    String COMFORT = "Удобства";

    String VIEW_ON_MAP = "Смотреть на карте";
    String ON_MAP_AREA = "Область на карте";
    String GEO_INPUT = "Адрес";
    String DISTRICT = "Район";
    String METRO = "Метро";
    String HIGHWAY = "Шоссе";

    String PRICE_FROM = "Цена от";
    String TO = "до";
    String AREA_FROM = "Площадь от";
    String AREA_MIN = "areaMin";
    String AREA_MAX = "areaMax";

    @Name("Блок цены>")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_price-with-type')]")
    InputField price();

    @Name("Саджест {{ value }}")
    @FindBy(".//ul[contains(@class, 'Suggest__list')]//li[contains(., '{{ value }}')]")
    AtlasWebElement suggest(@Param("value") String value);

    @Name("Саджест ")
    @FindBy(".//ul[contains(@class, 'Suggest__list')]//li")
    ElementsCollection<AtlasWebElement> suggest();

    @Name("Площадь")
    @FindBy(".//div[contains(@class, 'item_name_area')]")
    FieldSetBlock area();

    @Name("Площадь участка")
    @FindBy(".//div[contains(@class, 'item_name_lotArea')]")
    FieldSetBlock lotArea();

    @Name("Счётчик бэйджиков")
    @FindBy(".//div[contains(@class, 'FiltersFormField__refinements-badges')]//button")
    AtlasWebElement badgesCounter();

    @Name("Бэйджик «{{ value }}»")
    @FindBy("//div[contains(@class,'Popup_visible')]//div[contains(@class, 'GeoBadges__item')]" +
            "[contains(., '{{ value }}')]")
    Badge badges(@Param("value") String value);

    @Name("Бэйджик")
    @FindBy("//div[contains(@class,'Popup_visible')]//div[contains(@class, 'GeoBadges__item')]")
    ElementsCollection<AtlasWebElement> badges();

    @Name("Кнопка выбора расширенных фильтров")
    @FindBy(".//div[contains(@class, 'FiltersFormField_section_extra')]/span")
    RealtyElement showMoreFiltersButton();

    @Name("Кнопка «Показать»")
    @FindBy(".//div[contains(@class, 'FormField_name_submit') " +
            "and not(contains(@class, 'FiltersFormField__counter-submit_loading'))]")
    @Retry(polling = 10L)
    AtlasWebElement submitButton();

    @Name("Форма кнопки «Показать/Найти»")
    @FindBy(".//*[contains(@class, 'FormField_name_submit')]")
    @Retry(polling = 10L)
    AtlasWebElement showButtonForm();

    @Name("Доп кнопки в строке адреса")
    @FindBy("//div[@class='FiltersFormField__refinements-selector']")
    Link geoButtons();

    @Name("Блок фильтров  «{{ value }}»")
    @FindBy(".//div[contains(@class,'FiltersFormField_name_')][contains(.,'{{ value }}')]")
    AtlasWebElement filtersBlock(@Param("value") String value);

    default AtlasWebElement geoInput() {
        return input(GEO_INPUT);
    }

    default void submitWithWait() {
        showButtonForm().waitUntil(not(hasClass(containsString("FiltersFormField__counter-submit_loading"))));
        submitButton().click();
    }
}
