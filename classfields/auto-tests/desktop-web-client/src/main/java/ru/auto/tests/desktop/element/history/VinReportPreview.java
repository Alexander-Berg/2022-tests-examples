package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface VinReportPreview extends VertisElement, WithButton {

    String ONE_REPORT = "Один отчёт";
    String BUY_PACKAGE = "Купить пакет";

    @Name("Кнопки покупки")
    @FindBy(".//div[contains(@class, 'VinReportPreviewDesktop__buttons')]")
    VinReportPromoPurchase purchase();
}
