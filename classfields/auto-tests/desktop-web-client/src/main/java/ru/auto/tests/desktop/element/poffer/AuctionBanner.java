package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface AuctionBanner extends VertisElement, WithButton {

    String CREATE_REQUEST = "Оформить заявку";

    @Name("Диапазон цены")
    @FindBy(".//span[contains(@class, '_priceRange')]")
    VertisElement priceRange();

    @Name("Иконка помощи")
    @FindBy(".//div[contains(@class, '_helpIcon')]")
    VertisElement help();

}
