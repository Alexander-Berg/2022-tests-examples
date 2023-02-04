package ru.auto.tests.desktop.mobile.element.group;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.FilterButton;

public interface Filters extends VertisElement {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//div[.= '{{ text }}'] | " +
            ".//span[.= '{{ text }}'] |" +
            ".//div[contains(@class, 'PseudoInput ') and .= '{{ text }}']")
    FilterButton button(@Param("text") String text);
}