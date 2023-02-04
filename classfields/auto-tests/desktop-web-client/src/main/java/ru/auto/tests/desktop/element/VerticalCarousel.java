package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VerticalCarousel extends VertisElement {

    @Name("Список элементов")
    @FindBy(".//section[@class = 'ListingSameGroupItem'] | " +
            ".//section[@class = 'ListingPremiumItem'] | " +
            ".//li[@class = 'CarouselUniversal__item'] | " +
            ".//div[@class = 'VersusRelatedOffer']|" +
            ".//a[contains(@class, 'VersusRelatedOfferDesktop__link')]")
    ElementsCollection<VerticalCarouselItem> itemsList();

    @Name("Кнопка «Обновить»")
    @FindBy(".//button[contains(@class, 'UpdatableListOffers__updateButton')]")
    VertisElement updateButton();

    @Step("Получаем элемент с индексом {i}")
    default VerticalCarouselItem getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}