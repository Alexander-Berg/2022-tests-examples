package ru.yandex.realty.element.samolet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface FiltersBlock extends Button {

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
