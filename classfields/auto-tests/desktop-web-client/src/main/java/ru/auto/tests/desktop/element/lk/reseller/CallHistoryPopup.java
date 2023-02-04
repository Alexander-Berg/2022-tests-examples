package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface CallHistoryPopup extends VertisElement, WithButton {

    @Name("Список звонков")
    @FindBy(".//div[contains(@class, 'OfferCallHistory__item')]")
    ElementsCollection<VertisElement> phonesList();
}
