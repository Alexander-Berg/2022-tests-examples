package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Insurance extends VertisElement, WithButton {

    @Name("Кнопка редактирования страховки")
    @FindBy(".//button[contains(@class, 'GarageCardInsuranceMobile__button')]")
    VertisElement editButton();
}