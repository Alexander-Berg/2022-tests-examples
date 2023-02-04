package ru.auto.tests.desktop.mobile.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface VinReportPreview extends VertisElement, WithButton {

    @Name("Пакет «{{ text }}»")
    @FindBy(".//div[contains(@class, 'CardVinReportActionButtonMobile') and .= '{{ text }}']")
    VertisElement vinPackage(@Param("text") String text);
}