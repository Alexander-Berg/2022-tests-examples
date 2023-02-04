package ru.auto.tests.desktop.component.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.cabinet.Calendar;

public interface WithCalendar {

    @Name("Календарь")
    @FindBy("//div[contains(@class, 'Popup_visible')] | " +
            "//div[contains(@class, 'calendar')][contains(@class, 'popup_visible')] | " +
            "//div[contains(@class, 'DateRange__calendarContainer')]")
    Calendar calendar();
}