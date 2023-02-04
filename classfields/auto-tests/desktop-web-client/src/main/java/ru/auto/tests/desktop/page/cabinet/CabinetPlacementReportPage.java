package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivePopup;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.cabinet.WithCalendar;
import ru.auto.tests.desktop.element.cabinet.placement_report.Chart;
import ru.auto.tests.desktop.element.cabinet.placement_report.PlatformTable;
import ru.auto.tests.desktop.page.BasePage;

public interface CabinetPlacementReportPage extends BasePage, WithInput, WithButton, WithActivePopup, WithCalendar {

    @Name("Кнопка открытия календаря")
    @FindBy("//button[contains(@class, 'PlacementReportFilters__period')]")
    VertisElement calendarButton();

    @Name("Кнопка экспорта")
    @FindBy("//button[contains(@class, 'PlacementReportExport')]")
    VertisElement exportButton();

    @Name("Таблица «Показатели по площадкам»")
    @FindBy("//div[contains(@class, 'PlacementReportTable')]/table[contains(@class, 'PlacementReportTable__table')]")
    PlatformTable platformTable();

    @Name("График «{{ chartTitle }}»")
    @FindBy("//div[contains(@class, 'PlacementReport__block') and .//div[.='{{ chartTitle }}']]")
    Chart chart(@Param("chartTitle") String chartTitle);
}
