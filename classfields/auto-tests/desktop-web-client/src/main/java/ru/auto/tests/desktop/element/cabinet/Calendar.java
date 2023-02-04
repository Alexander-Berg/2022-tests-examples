package ru.auto.tests.desktop.element.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public interface Calendar extends VertisElement {

    @Name("Название предыдущего периода (месяц и год)")
    @FindBy(".//div[@data-visible = 'true']//div[contains(@class, 'CalendarMonth_caption')]")
    VertisElement previousMonthAndYear();

    @Name("Месяц")
    @FindBy(".//div[contains(@class, 'CalendarMonth') and @data-visible = 'true'] | " +
            ".//div[contains(@class, 'calendar__name')] | " +
            ".//div[contains(@class, 'CalendarMonthGrid__horizontal')]")
    CalendarMonth month();

    @Name("Пресет «{{ text }}»")
    @FindBy(".//button[contains(@class, 'DateRangePresets__item') and .//span[.= '{{ text }}']]")
    VertisElement preset(@Param("text") String text);

    @Step("Выбираем период {{ firstDay }} - {{ lastDay }}")
    default void selectPeriod(String firstDay, String lastDay) {
        month().day(firstDay).click();
        waitSomething(1, TimeUnit.SECONDS);
        month().day(lastDay).click();
    }

}
