package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FilterChip extends VertisElement, Link {

    String RESET_ALL = "Сбросить все";

    @Name("Кнопка сброса")
    @FindBy(".//div[contains(@class, '_iconClose_')]")
    VertisElement reset();

}
