package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface DailyOffer extends VertisElement, WithButton {

    @Name("Иконка телефона")
    @FindBy(".//div[contains(@class, 'ListingItemPremium__contactsRight')]")
    VertisElement phoneIcon();
}