package ru.auto.tests.desktop.element.cabinet.backonsale;


import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;

public interface SortsBlock extends VertisElement, WithSelect, WithCheckbox {

    @Name("Кнопка открытия календаря")
    @FindBy(".//button[contains(@class, 'DateRange__button')]")
    VertisElement calendarButton();
}
