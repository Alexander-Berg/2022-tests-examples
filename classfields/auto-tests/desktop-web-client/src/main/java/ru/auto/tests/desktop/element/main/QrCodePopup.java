package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface QrCodePopup extends VertisElement {

    @Name("QR code")
    @FindBy(".//img")
    VertisElement img();
}
