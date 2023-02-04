package ru.yandex.realty.element.offers;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.RealtyElement;

public interface PayButton extends RealtyElement {

    @Name("Скрытая цена")
    @FindBy("//s")
    AtlasWebElement saveTotal();
}
