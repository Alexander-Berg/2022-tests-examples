package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.card.VinReport;

public interface WithVinReport {

    @Name("Отчёт о проверке по VIN")
    @FindBy("//div[contains(@class, 'CardVinReportTemplate')]")
    VinReport vinReport();
}