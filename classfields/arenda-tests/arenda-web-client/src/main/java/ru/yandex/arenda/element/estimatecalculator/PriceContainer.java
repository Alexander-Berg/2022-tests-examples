package ru.yandex.arenda.element.estimatecalculator;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Button;

public interface PriceContainer extends Button {

    String RENT_WITH_BUTTON = "Сдать с\u00a0помощью";

    @Name("Кнопка «Оценить другую»")
    @FindBy(".//div[contains(@class,'LandingCalculatorArendaRentPrice__estimateButton')]")
    AtlasWebElement estimateOther();
}
