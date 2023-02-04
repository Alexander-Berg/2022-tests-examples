package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectionBlock;
import ru.yandex.realty.mobile.element.Fieldset;

public interface NewBuildingCardPlans extends Button, SelectionBlock, Fieldset, InputField {

    @Name("Фильтры2.0")
    @FindBy("//*[contains(@class,'IconSvg_filters-24')]")
    AtlasWebElement extFiltersV2();


}

