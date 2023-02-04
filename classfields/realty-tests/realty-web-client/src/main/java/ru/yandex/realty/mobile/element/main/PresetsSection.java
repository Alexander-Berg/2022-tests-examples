package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import ru.yandex.realty.mobile.element.Link;
import ru.yandex.realty.mobile.element.Slider;

public interface PresetsSection extends AtlasWebElement, Link, Slider {

    String BUY_FLAT = "Купить квартиру";
    String NOVOSTROJKI = "Новостройки";
    String RENT_FLAT = "Снять квартиру";
    String RENT_PER_DAY = "Снять посуточно";
    String COMMERCIAL = "Коммерческая недвижимость";
    String COUNTRYSIDE = "Загородная недвижимость";
    String GARAGES = "Гаражи";

}
