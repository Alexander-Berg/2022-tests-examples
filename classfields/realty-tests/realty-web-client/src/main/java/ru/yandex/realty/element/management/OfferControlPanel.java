package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface OfferControlPanel extends Button, Link {

    @Name("Продлить оффер")
    @FindBy(".//button[contains(@class, 'prolongate-button')]")
    AtlasWebElement prolongateOffer();
}
