package ru.auto.tests.desktop.element.cabinet.calls;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 24.05.18
 */
public interface Stats extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'CallsTotalStats__title')]")
    VertisElement title();

    @Name("Подсказка")
    @FindBy(".//div[contains(@class, 'calls__chart-wtf')]")
    VertisElement help();
}