package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface AgencyOfferInfo extends AtlasWebElement {

    @Name("Ссылка оффера")
    @FindBy(".//a")
    AtlasWebElement offerLink();
}
