package ru.auto.tests.desktop.element.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface HeaderLine2 extends VertisElement, WithButton {

    @Name("Кнопка «{{ text }}» с прыщём")
    @FindBy(".//li[@class = 'HeaderMainNav__item HeaderMainNav__item_dot' and .= '{{ text }}']")
    VertisElement buttonWithDot(@Param("text") String text);

    @Name("Выпадушка")
    @FindBy(".//li[contains(@class, 'Dropdown_visible')] | " +
            ".//div[contains(@class, 'HeaderMainNav__menu_opened')]")
    HeaderDropdown dropdown();
}