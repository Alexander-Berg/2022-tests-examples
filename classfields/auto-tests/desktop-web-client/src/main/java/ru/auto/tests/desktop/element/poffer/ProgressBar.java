package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface ProgressBar extends VertisElement, WithButton {

    @Name("Икнока ?")
    @FindBy(".//a[contains(@class, 'dropdown')]")
    VertisElement helpIcon();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//div[.= '{{ text }}']")
    VertisElement button(@Param("text") String text);
}