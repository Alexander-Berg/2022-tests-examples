package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectionBlock;

public interface MobilePopup extends InputField, Button, CloseCross, SelectionBlock {

    @Name("Тумблер {{ value }}")
    @FindBy(".//div[contains(@class,'Tumbler') and contains(@class,'Control') and contains(.,'{{ value }}')]")
    AtlasWebElement tumbler(@Param("value") String value);
}
