package ru.yandex.realty.mobile.element.specproject;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.SelectionBlock;

public interface SpecProjectFiltersBlock extends SelectionBlock, Button {

    @Name("Кнопка «Найти»")
    @FindBy(".//button[contains(@class,'SamoletFilters__submitBtn')]")
    AtlasWebElement submitButton();

    @Name("Крестик «закрыть фильтры»")
    @FindBy("//*[contains(@class, 'CrossIconHeader__icon')]")
    AtlasWebElement closeButton();

    @Name("Кнопка выбора срока сдачи")
    @FindBy("//button[contains(@class,'Select__button')]")
    AtlasWebElement deliveryDateButton();

    @Name("Опция «{{ value }}» селектора срока сдачи")
    @FindBy("//div[contains(@class, 'Menu__item_mode_radio') and contains(.,'{{ value }}')]")
    AtlasWebElement deliveryDateOption(@Param("value") String value);

    @Name("Цена от")
    @FindBy(".//input[@id='price-range_from']")
    AtlasWebElement priceFrom();

    @Name("Цена до")
    @FindBy(".//input[@id='price-range_to']")
    AtlasWebElement priceTo();

    @Name("Площадь от")
    @FindBy(".//input[@id='area-range_from']")
    AtlasWebElement areaFrom();

    @Name("Площадь до")
    @FindBy(".//input[@id='area-range_to']")
    AtlasWebElement areaTo();
}
