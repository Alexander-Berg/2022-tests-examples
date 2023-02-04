package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Chips extends VertisElement, Button, Link {

    @Name("Кнопка сброса")
    @FindBy(".//button[contains(@class, '_erase')]")
    VertisElement reset();

}
