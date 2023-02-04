package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.ipoteka.BankOffer;
import ru.yandex.realty.mobile.element.ipoteka.IpotekaOffer;

public interface IpotekaCalculatorPage extends BasePage, Button {

    @Name("Бегунок «{{ value }}»")
    @FindBy("//div[./label[text()='{{ value }}']]//div[@class='SliderInputPromo__handle']")
    AtlasWebElement runner(@Param("value") String value);

    @Name("Бегунок «{{ value }}»")
    @FindBy("//div[./label[text()='{{ value }}']]//input")
    AtlasWebElement input(@Param("value") String value);

    @Name("Калькулятор -> фильтры")
    @FindBy("//div[contains(@class, 'MortgageCalculatorCard__calculator')]")
    AtlasWebElement mortgageSearchFilters();

    @Name("Калькулятор -> результат")
    @FindBy("//div[contains(@class, 'MortgageCalculatorCard__offers')]")
    AtlasWebElement mortgageResults();

    @Name("Офферы ипотеки")
    @FindBy("//div[contains(@class, 'OfferSliderSnippet__container')]")
    ElementsCollection<IpotekaOffer> ipotekaOffer();

    @Name("Программа банка")
    @FindBy("//div[contains(@class, 'MortgageProgramsSerp__snippet')]")
    ElementsCollection<BankOffer> bankOffers();

    @Name("Альфамодуль")
    @FindBy(".//div[contains(@class,'AlfaBankMortgageFormScreen__modal')]")
    Button alfaModal();
}