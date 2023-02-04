package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface OfferServicesPanel extends AtlasWebElement {

    String ADD_SERVICE = "Подключить";
    String ADD_SERVICE_FREE = "Подключить бесплатно";
    String PREMIUM = "Премиум";
    String PROMOTION = "Продвижение";
    String TURBO = "Турбо";

    @Name("Блок «{{ value }}»")
    @FindBy(".//div[@class='VasPanel__service' and contains(., '{{ value }}')]")
    OfferService service(@Param("value") String value);
}
