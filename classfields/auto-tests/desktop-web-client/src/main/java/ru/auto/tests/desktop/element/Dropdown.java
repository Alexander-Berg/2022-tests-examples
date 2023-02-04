package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.listing.DropdownItem;

public interface Dropdown extends VertisElement, WithButton {

    @Name("Элемент {{ text }} в выпадающем списке")
    @FindBy(".//div[contains(@class,'MenuItem') and . = '{{ text }}']")
    VertisElement item(@Param("text") String linkText);

    @Name("«+» на элементе {{ text }} в выпадающем списк")
    @FindBy(".//div[@class = 'MenuItemGroup' and . = '{{ text }}']//button")
    DropdownItem nameplateButton(@Param("text") String linkText);
}
