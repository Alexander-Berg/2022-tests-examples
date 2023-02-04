package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BannedMessage extends VertisElement {

    @Name("Кнопка «Написать в поддержку»")
    @FindBy(".//a[contains(@class, 'banned__support-button')]")
    VertisElement supportButton();
}
