package ru.yandex.realty.element.offers;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface InnerContent extends AtlasWebElement {

    @Name("Контент")
    @FindBy(".//div[@class='offer-form-group__inner']")
    AtlasWebElement formInner();
}
