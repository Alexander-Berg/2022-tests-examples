package ru.auto.tests.desktop.element.garage;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface CardPromos extends VertisElement, WithButton {

    @Name("Список супер промо")
    @FindBy(".//div[contains(@class, 'GarageCardPromos__superItem')]")
    ElementsCollection<PromoItem> superPromoList();

    @Name("Список обычных промо")
    @FindBy(".//li[contains(@class, 'GarageCardPromos__item')]")
    ElementsCollection<PromoItem> regularPromoList();

}
