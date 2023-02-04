package ru.yandex.realty.element.offers;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface ServicePayment extends AtlasWebElement {

    String ACTIVATE = "Подключить";
    String PROMOTION = "Продвижение";
    String RENEW_FOR = "Продлить за";

    @Name("Сервис {{ value }}")
    @FindBy(".//div[@class='VasService' and contains(.,'{{ value }}')]")
    Button service(@Param("value") String value);
}
