package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface PricePopup extends VertisElement, WithButton {

    @Name("Сниженная цена")
    @FindBy(".//div[contains(@class, 'itemDiff_fallen')]")
    VertisElement loweredPrice();

    @Name("График")
    @FindBy(".//img[contains(@class, 'GreatDealGraph__img')]")
    VertisElement graph();

    @Name("Предложение кредита")
    @FindBy(".//span[@class = 'OfferPricePopupContent__credit'] | " +
            ".//span[contains(@class, 'CreditPrice_type_link')]")
    VertisElement creditOffer();
}