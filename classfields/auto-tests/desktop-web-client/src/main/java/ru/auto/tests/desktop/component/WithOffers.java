package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.HorizontalCarousel;

public interface WithOffers {

    @Name("Предложения о продаже")
    @FindBy("//div[contains(@data-bem, 'catalog-related-offers')] | " +
            "//div[contains(@class, '__saleCarousel')]")
    HorizontalCarousel offers();
}