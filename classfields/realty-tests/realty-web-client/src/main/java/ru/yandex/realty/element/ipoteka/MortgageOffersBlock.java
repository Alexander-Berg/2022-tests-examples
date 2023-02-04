package ru.yandex.realty.element.ipoteka;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface MortgageOffersBlock extends AtlasWebElement {

    @Name("Список офферов")
    @FindBy(".//div[contains(@class,'MortgageOffers__snippet')]")
    ElementsCollection<MortgageOffer> mortgageOffersList();

    default MortgageOffer offer(int i) {
        return mortgageOffersList().should(hasSize(greaterThan(i))).get(i);
    }
}
