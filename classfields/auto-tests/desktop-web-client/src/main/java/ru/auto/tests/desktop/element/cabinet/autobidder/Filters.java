package ru.auto.tests.desktop.element.cabinet.autobidder;

import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;

public interface Filters extends VertisElement, WithInput, WithSelect, WithRadioButton {

    String PRICE_FROM = "Цена от, ₽";
    String YEAR_FROM = "Год от";
    String IN_STOCK = "В наличии";
    String ON_THE_WAY = "В пути";
    String VIN = "VIN номер или несколько через запятую";
    String CHECKS_BY_VIN = "Проверки по VIN";

}
