package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface OfferStat extends AtlasWebElement {

    @Name("Подключите отслеживание звонков")
    @FindBy(".//button[@class='offer-preview-calls-placeholder__enable-call-stats']")
    AtlasWebElement enableCalls();
}
