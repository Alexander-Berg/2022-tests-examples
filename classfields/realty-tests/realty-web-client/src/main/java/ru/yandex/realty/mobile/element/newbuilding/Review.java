package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;

public interface Review extends Link {

    @Name("Звездочка «{{ value }}»")
    @FindBy(".//div[@data-rating='{{ value }}']")
    AtlasWebElement star(@Param("value") String value);

    @Name("Звездочки")
    @FindBy(".//div[@data-rating]")
    ElementsCollection<AtlasWebElement> stars();
}
