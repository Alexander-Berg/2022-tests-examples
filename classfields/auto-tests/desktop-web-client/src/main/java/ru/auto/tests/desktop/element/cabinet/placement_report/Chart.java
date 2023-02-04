package ru.auto.tests.desktop.element.cabinet.placement_report;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Chart extends VertisElement {

    @Name("Иконка подсказки")
    @FindBy(".//div[contains(@class, 'PlacementReportGraphHead')]//div[contains(@class, 'HoveredTooltip__trigger')]")
    VertisElement tooltipIcon();

    @Name("Кнопка «Подробнее»")
    @FindBy(".//div[contains(@class, 'WalkInCollapseBlock')]/span[contains(@class, 'WalkInCollapseBlock__collapseButton')]")
    VertisElement walkInCollapseButton();

    @Name("Легенда графика «Приезды в салон с Авто.ру»")
    @FindBy(".//div[contains(@class, 'WalkInCollapseBlock')]/div[contains(@class, 'PlacementReportWalkIn__legendContent')]")
    VertisElement walkInLegend();
}
