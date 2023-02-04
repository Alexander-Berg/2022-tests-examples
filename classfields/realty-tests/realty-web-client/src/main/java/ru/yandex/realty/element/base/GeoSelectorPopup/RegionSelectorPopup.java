package ru.yandex.realty.element.base.GeoSelectorPopup;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;

public interface RegionSelectorPopup extends Button, InputField {

    String SAVE = "Сохранить";

    @Name("Хлебная крошка «{{ value }}»")
    @FindBy(".//span[contains(@class, 'BreadcrumbItem_withSeparator') and contains(., '{{ value }}')]")
    AtlasWebElement breadCrumb(@Param("value") String value);

    @Name("Хлебные крошки")
    @FindBy(".//span[contains(@class, 'BreadcrumbItem_withSeparator')]")
    ElementsCollection<AtlasWebElement> breadCrumbs();

    @Name("Саждест «{{ value }}»")
    @FindBy(".//li[text()='{{ value }}']")
    AtlasWebElement suggestItem(@Param("value") String value);

    @Name("Текущий регион")
    @FindBy("//div[contains(@class,'CurrentRegion__name')]")
    AtlasWebElement regionName();
}
