package ru.auto.tests.desktop.element.cabinet.booking;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BookingItem extends VertisElement {

    @Name("Название")
    @FindBy(".//a[contains(@class, '__title')]")
    VertisElement title();

    @Name("Кнопка телефона")
    @FindBy(".//button[contains(@class, 'BookingItem__bookerPhone')]")
    VertisElement phoneButton();

    @Name("Статус «{{ text }}»")
    @FindBy("//div[.= '{{ text }}']")
    VertisElement status(@Param("text") String text);

    @Name("Иконка ?")
    @FindBy(".//div[contains(@class, 'priceTooltip')]")
    VertisElement helpIcon();
}