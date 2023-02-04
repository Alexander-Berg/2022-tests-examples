package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public interface FiltersPopup extends Popup, Input, Checkbox {

    String SHOW_BUTTON = "Показать";
    String REGION = "Регион или город";
    String METRO_STREET_DISTRICT = "Метро, улица, район";
    String ADDRESS_AND_RADIUS = "Адрес и радиус поиска";
    String DISTRICT = "Район";
    String METRO = "Метро";
    String STATION_OR_LINE = "Станция метро или ветка";
    String DISTRICT_NAME = "Название района";
    String BU = "Б/У";
    String NOVIY = "Новый";

    @Name("Блок фильтров «{{ value }}»")
    @FindBy(".//div[contains(@class, 'OfferFilterForm') and contains(@class, '_wrapper')][.//span[.='{{ value }}']]")
    FilterBlock filterBlock(@Param("value") String value);

    @Name("Сбросить")
    @FindBy(".//div[contains(@class,'FilterHeaderButton__wrapper')]/span")
    VertisElement cancel();

    @Name("Кнопка «Показать объявления»")
    @FindBy(".//div[contains(@class, 'FilterFooterButton')]")
    VertisElement showOffers();

    @Name("Сортировка")
    @FindBy(".//div[contains(@class, 'FilterForm__sort')]")
    Link sortBlock();

    @Name("Активный бабл сортировки")
    @FindBy(".//div[contains(@class, 'FilterFormSort')]//div[contains(@class, 'Tag__active')]")
    VertisElement activeSort();

    @Name("Тайтл экрана")
    @FindBy(".//span[contains(@class, 'Screen__title')]")
    VertisElement screenTitle();

    @Name("Слайдер радиуса")
    @FindBy(".//div[contains(@class, '_sliderWrapper')]")
    VertisElement radiusSlider();

    @Name("Селектор категории")
    @FindBy(".//div[contains(@class, 'FilterFormCategory')]")
    CategorySelector categorySelector();

}
