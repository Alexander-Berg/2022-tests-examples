package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface VinReportScore extends VertisElement, WithInput {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//div[contains(@class, 'VinHistoryScore__scoreButton') and .= '{{ text }}']")
    VertisElement button(@Param("text") String text);
}