package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PriceBlock extends Block {

    @Name("Икнока ?")
    @FindBy(".//a[contains(@class, 'price__question')]")
    VertisElement helpIcon();

    @Name("Средняя цена")
    @FindBy(".//div[contains(@class, 'price__recommended')]")
    VertisElement averagePrice();
}