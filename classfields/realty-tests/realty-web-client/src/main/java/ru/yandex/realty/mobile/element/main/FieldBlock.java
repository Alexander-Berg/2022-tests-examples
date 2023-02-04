package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.mobile.element.ButtonWithText;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface FieldBlock extends InputField, Button, ButtonWithText {

    @Name("Значение фильтра")
    @FindBy(".//div[contains(@class, 'FiltersFormFieldOpener__label')]")
    AtlasWebElement value();

    @Name("Тумблер {{ value }}")
    @FindBy(".//div[contains(@class, 'TumblersetFormControl__item') and contains(., '{{ value }}')]")
    AtlasWebElement tumbler(@Param("value") String value);

}
