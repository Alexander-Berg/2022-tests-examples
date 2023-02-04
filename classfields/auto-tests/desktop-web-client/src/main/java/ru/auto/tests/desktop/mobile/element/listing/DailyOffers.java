package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DailyOffers extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'ListingPremiumOffers__title')]")
    VertisElement title();

    @Name("Список предложений дня")
    @FindBy(".//div[@class = 'ListingPremiumOffers__offer']")
    ElementsCollection<DailyOffer> dailyOffersList();

    @Step("Получаем предложение дня с индексом {i}")
    default DailyOffer getDailyOffer(int i) {
        return dailyOffersList().should(hasSize(greaterThan(i))).get(i);
    }
}