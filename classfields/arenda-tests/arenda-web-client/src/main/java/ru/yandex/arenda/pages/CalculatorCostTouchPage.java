package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface CalculatorCostTouchPage extends CalculatorCostPage {

    @Name("Выбор комнатности -  «{{ value }}»")
    @FindBy(".//div[contains(@class, 'LandingCommonNumberOfRoomsRadioGroup__item') and contains(.,'{{ value }}')]")
    AtlasWebElement roomsCount(@Param("value") String value);
}
