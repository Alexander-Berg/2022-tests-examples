package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithSelect;

public interface BestOfferPopup extends VertisElement, WithSelect, WithInput, WithButton {

    @Name("Результат заявки")
    @FindBy(".//div[contains(@class, 'MatchApplicationModalNotification__content')]")
    VertisElement result();
}
