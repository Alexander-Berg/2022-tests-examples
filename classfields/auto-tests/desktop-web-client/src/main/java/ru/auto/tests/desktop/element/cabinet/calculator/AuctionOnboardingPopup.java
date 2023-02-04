package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface AuctionOnboardingPopup extends VertisElement, WithButton {

    @Name("Текст поп-апа")
    @FindBy(".//div[@class='OnboardingPopup__description']")
    VertisElement text();

}
