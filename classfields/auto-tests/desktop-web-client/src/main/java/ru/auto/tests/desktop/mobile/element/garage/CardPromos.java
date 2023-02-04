package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardPromos extends VertisElement {

    @Name("Список промо айтемов")
    @FindBy(".//div[contains(@class, ' GaragePromoListScrollable__item')]")
    ElementsCollection<VertisElement> items();

}
