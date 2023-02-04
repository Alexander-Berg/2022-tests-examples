package ru.yandex.realty.element.ipoteka;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.InputField;

public interface MortgageCalc extends InputField {

    @Name("Поле «Цена недвижимости»")
    @FindBy(".//div[contains(@class,'MortgageCalculator__costInput')]//input")
    AtlasWebElement costInput();

    @Name("Срок кредита")
    @FindBy(".//div[contains(@class,'MortgageCalculator__periodInput')]//input")
    AtlasWebElement inputCreditTerm();

    @Name("Первоначальный взнос")
    @FindBy(".//div[contains(@class,'MortgageCalculator__downpaymentInput')]//input")
    AtlasWebElement downpaymentInput();

    @Name("Ставка")
    @FindBy(".//div[contains(@class,'MortgageCalculator__rateInput')]//input")
    AtlasWebElement rateInput();

}
