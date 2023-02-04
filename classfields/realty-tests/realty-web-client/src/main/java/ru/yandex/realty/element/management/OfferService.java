package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface OfferService extends Button {

    @Name("Кнопка ппродлить")
    @FindBy(".//button[contains(.,'Продлить')]")
    AtlasWebElement renewalButton();

    @Name("Чекбокс автопродление")
    @FindBy(".//label[contains(.,'Автопродление')]")
    AtlasWebElement autoRenew();
}
