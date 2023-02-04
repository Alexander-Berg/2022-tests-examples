package ru.yandex.realty.mobile.element.specproject;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface SpecProjOffer extends AtlasWebElement {

    @Name("ссылка оффера")
    @FindBy(".//a[contains(@class,'Link_js_inited')]")
    AtlasWebElement offerLink();
}
