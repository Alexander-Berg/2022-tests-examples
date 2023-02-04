package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.RealtyElement;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SelectPopup extends RealtyElement, SelectionBlock, RadioButton {

    @Name("Пункт «{{ value }}»")
    @FindBy(".//div[contains(@class, 'Menu__item')][contains(., '{{ value }}')]")
    RealtyElement item(@Param("value") String value);

    @Name("Список вариантов")
    @FindBy(".//div[contains(@class, 'Menu__item')]")
    ElementsCollection<AtlasWebElement> items();
}
