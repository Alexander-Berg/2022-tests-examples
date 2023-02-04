package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DataPickerMonth extends VertisElement {

    @Name("День «{{ value }}»")
    @FindBy(".//td[contains(@class, 'CalendarDay')][./span[text() = '{{ value }}']]")
    VertisElement day(@Param("value") String value);

    @Name("Первый день выбранного диапазона")
    @FindBy(".//td[contains(@class, 'CalendarDay__selected_start')]")
    VertisElement startDay();

    @Name("Последний день выбранного диапазона")
    @FindBy(".//td[contains(@class, 'CalendarDay__selected_end')]")
    VertisElement endDay();

}
