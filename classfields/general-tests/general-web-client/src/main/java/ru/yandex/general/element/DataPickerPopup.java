package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public interface DataPickerPopup extends VertisElement {

    String WEEK = "Неделя";
    String MONTH = "Месяц";

    @Name("Кнопка периода «{{ value }}»")
    @FindBy(".//label[contains(@class, 'StatsCalendarInfo__chip')][contains(., '{{ value }}')]")
    VertisElement periodChip(@Param("value") String value);

    @Name("Месяц «{{ value }}»")
    @FindBy(".//div[contains(@class, 'CalendarMonthGrid_month')][contains(., '{{ value }}')]")
    DataPickerMonth month(@Param("value") String value);

    @Name("Стрелка влево")
    @FindBy(".//button[contains(@class, '_arrowBtnLeft')]")
    VertisElement arrowLeft();

    @Name("Стрелка вправо")
    @FindBy(".//button[contains(@class, '_arrowBtnRight')]")
    VertisElement arrowRight();

    @Name("Список выбранных дней")
    @FindBy(".//td[contains(@class, 'CalendarDay__selected')]")
    ElementsCollection<VertisElement> selectedDays();

    default void clickDay(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat month = new SimpleDateFormat("MMMM", locale);
        SimpleDateFormat day = new SimpleDateFormat("d", locale);

        month(month.format(date).toLowerCase()).day(day.format(date)).click();
    }

}
