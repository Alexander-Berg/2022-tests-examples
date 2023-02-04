package ru.yandex.realty.mobile.element.ipoteka;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

public interface MortgageSearchFilters extends Button {

    @Name("Итем селектора «{{ value }}»")
    @FindBy("//div[contains(@class,'Menu__item_theme_realty') and contains(.,'{{ value }}')]")
    AtlasWebElement selectorItem(@Param("value") String value);

}
