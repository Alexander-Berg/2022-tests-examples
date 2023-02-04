package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface ActivatePopup extends VertisElement, WithButton {

    @Name("Кнопка «Разместить»")
    @FindBy(".//button[contains(@class, 'OfferActivateDialog__button')]")
    VertisElement activateButton();
}