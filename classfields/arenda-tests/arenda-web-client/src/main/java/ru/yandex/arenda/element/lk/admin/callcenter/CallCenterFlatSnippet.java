package ru.yandex.arenda.element.lk.admin.callcenter;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface CallCenterFlatSnippet extends AtlasWebElement {

    @Name("Адрес сниппета")
    @FindBy(".//h3[contains(@class,'OutstaffCallCenterFlatSnippet__address')]")
    AtlasWebElement snippetAddress();
}
