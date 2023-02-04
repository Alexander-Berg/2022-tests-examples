package ru.auto.tests.desktop.element.cabinet.agency.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.09.18
 */
public interface SearchClientPopup extends VertisElement {

    @Name("Поле ввода id клиента")
    @FindBy(".//input")
    VertisElement input();

    @Name("Кнопка «Найти»")
    @FindBy("//span[contains(@class, 'Button__content')]")
    VertisElement find();
}
