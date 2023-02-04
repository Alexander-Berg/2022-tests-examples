package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.PromoLandingBanner;

public interface PromoCarsLandingPage extends BasePage {

    String PHONE_NUMBER = "Номер телефона";
    String GET_BEST_PRICE = "Получить лучшую цену";

    @Name("Банер")
    @FindBy("//div[@class = 'NewCarsLandingBanner']")
    PromoLandingBanner banner();

    @Name("Снипет")
    @FindBy("//li[@class = 'PageNewCarsPromoLanding__snippet']")
    ElementsCollection<VertisElement> snippet();

    @Name("Большой снипет модели")
    @FindBy("//li[@class = 'PageNewCarsPromoLanding__majorSnippet']")
    VertisElement majorSnippet();

}
