package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Menu extends VertisElement {

    @Name("Пункт меню '{{ text }}'")
    @FindBy(".//div[contains(@class, 'MenuItem') and .= '{{ text }}']")
    VertisElement menuItem(@Param("text") String text);
}