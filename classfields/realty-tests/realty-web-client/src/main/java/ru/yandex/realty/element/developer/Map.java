package ru.yandex.realty.element.developer;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Map extends AtlasWebElement {

    @Name("Пины с типом = «{{ value }}» на карте")
    @FindBy("//a[contains(@class,'MapPlacemarkSerp')][contains(@class, 'type_{{ value }}')]")
    ElementsCollection<VertisElement> pinsWithType(@Param("value") String value);

    @Name("Пины на карте")
    @FindBy("//a[contains(@class,'MapPlacemarkSerp')]")
    ElementsCollection<VertisElement> pins();

}
