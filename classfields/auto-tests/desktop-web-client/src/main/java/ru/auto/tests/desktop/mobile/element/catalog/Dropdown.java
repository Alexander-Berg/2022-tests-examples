package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Dropdown extends VertisElement {

    @Name("Элемент меню «{{ text }}»")
    @FindBy(".//div[contains(@class,'MenuItem') and .='{{ text }}']")
    VertisElement item(@Param("text") String linkText);
}
