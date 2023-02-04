package ru.auto.tests.desktop.element.cabinet.sidebar;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 25.04.18
 */
//div[contains(@class, 'sidebar-cabinet')] может быть стоит добавить ul
public interface Sidebar extends VertisElement {

    @Name("Пункт меню «{{ name }}»")
    @FindBy(".//div[contains(@class, 'Sidebar__item') and .= '{{ name }}'] | " +
            ".//div[@class = 'Dropdown' and .= '{{ name }}'] | " +
            ".//a[contains(@class, 'Sidebar__item') and .= '{{ name }}']")
    VertisElement item(@Param("name") String name);

    @Name("Пункт подменю «{{ name }}»")
    @FindBy("//div[contains(@class, 'Sidebar__submenu')]//a[.= '{{ name }}']")
    VertisElement submenuItem(@Param("name") String name);
}