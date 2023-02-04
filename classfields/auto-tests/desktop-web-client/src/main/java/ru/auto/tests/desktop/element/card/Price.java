package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Price extends VertisElement, WithButton {

    @Name("Цена в рублях")
    @FindBy(".//div[contains(@class, '-module__price')] | " +
            ".//span[contains(@class, 'OfferPriceCaption__price')]")
    VertisElement rubPrice();

    @Name("Бейдж «Хорошая/отличная цена»")
    @FindBy(".//div[contains(@class, 'OfferPriceBadge')]")
    VertisElement greatDealBadge();

    @Name("Иконка сниженной цены")
    @FindBy(".//*[contains(@class, 'PriceUsedOffer__iconDiscount')]")
    VertisElement loweredPriceIcon();
}