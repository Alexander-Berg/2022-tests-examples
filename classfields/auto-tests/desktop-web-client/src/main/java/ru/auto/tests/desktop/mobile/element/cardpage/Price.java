package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Price extends VertisElement {

    @Name("Кнопка открытия поп-апа")
    @FindBy(".//div[contains(@class, 'CardPrice__control')]")
    VertisElement button();

    @Name("Бейдж «Заблокировано»")
    @FindBy(".//span[contains(@class, 'bannedBadge')]")
    VertisElement bannedBadge();

    @Name("Бейдж «Хорошая/отличная цена»")
    @FindBy(".//div[contains(@class, 'OfferPriceBadge')]")
    VertisElement greatDealBadge();

}
