package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.ipoteka.MortgageSearchFilters;

public interface IpotekaPage extends BasePage, Button {

    String ANY_PROGRAM_BUTTON = "Любой тип ипотеки";

    @Name("Калькулятор")
    @FindBy(".//div[contains(@class,'MortgageSearch__filters')]")
    MortgageSearchFilters filters();

    @Name("Блок фастлинков")
    @FindBy("//div[contains(@class,'MortgageSearchPresets__presets')]")
    MortgageSearchFilters fastlinks();
}