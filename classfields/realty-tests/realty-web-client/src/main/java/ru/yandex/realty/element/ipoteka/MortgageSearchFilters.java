package ru.yandex.realty.element.ipoteka;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

public interface MortgageSearchFilters extends Button {

    @Name("Итем селектора «{{ value }}»")
    @FindBy("//div[contains(@class,'Menu__item_theme_realty') and contains(.,'{{ value }}')]")
    AtlasWebElement selectorItem(@Param("value") String value);

    @Name("Пресет «{{ value }}»")
    @FindBy("//div[contains(@class,'MortgageSearchPresets__item') and contains(.,'{{ value }}')]")
    Link preset(@Param("value") String value);

    @Name("Фастлинк «{{ value }}»")
    @FindBy("//button[contains(@class,'MortgageSearchPresets__tag') and contains(text(),'{{ value }}')]")
    Link fastlink(@Param("value") String value);

}
