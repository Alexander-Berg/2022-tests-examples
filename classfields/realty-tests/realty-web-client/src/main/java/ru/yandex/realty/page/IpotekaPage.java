package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ipoteka.MortgageSearchFilters;
import ru.yandex.realty.mobile.page.BasePage;

public interface IpotekaPage extends BasePage {

    String ANY_PROGRAM_BUTTON = "Любой тип ипотеки";

    @Name("Калькулятор")
    @FindBy(".//div[contains(@class,'MortgageSearch__filters')]")
    MortgageSearchFilters filters();
}
