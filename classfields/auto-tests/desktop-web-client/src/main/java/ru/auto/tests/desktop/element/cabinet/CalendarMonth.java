package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CalendarMonth extends VertisElement {

    @Name("Вчерашний день на календаре")
    @FindBy(".//td[contains(@class, 'CalendarDay__today')]/preceding-sibling::td[1]")
    VertisElement prevDay();

    @Name("Сегодняшний день на календаре")
    @FindBy(".//td[contains(@class, 'CalendarDay__today')]")
    VertisElement currentDay();

    @Name("Завтрашний день на календаре")
    @FindBy(".//td[contains(@class, 'CalendarDay__today')]/following-sibling::td")
    VertisElement nextDay();

    @Name("Первый день недели")
    @FindBy(".//td[contains(@class, 'CalendarDay__firstDayOfWeek')]")
    VertisElement firstDayOfWeek();

    @Name("Последний день недели")
    @FindBy(".//td[contains(@class, 'CalendarDay__lastDayOfWeek')]")
    VertisElement lastDayOfWeek();

    @Name("Число «{{ text }}»")
    @FindBy(".//td[contains(@class, 'CalendarDay__default') and .= '{{ text }}'] | " +
            ".//td[contains(@aria-label, '{{ text }}')]")
    VertisElement day(@Param("text") String text);
}