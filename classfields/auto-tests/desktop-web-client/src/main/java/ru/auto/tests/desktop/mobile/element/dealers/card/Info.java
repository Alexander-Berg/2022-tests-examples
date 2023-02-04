package ru.auto.tests.desktop.mobile.element.dealers.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface Info extends VertisElement, WithButton {

    @Name("Статус официального дилера")
    @FindBy(".//div[contains(@class, 'DealerInfo__status')]")
    VertisElement officialDealerStatus();

    @Name("Контролы на Я.картах")
    @FindBy(".//ymaps[contains(@class, 'ymaps_maps-zoom')]")
    VertisElement yaMapsControls();
}
