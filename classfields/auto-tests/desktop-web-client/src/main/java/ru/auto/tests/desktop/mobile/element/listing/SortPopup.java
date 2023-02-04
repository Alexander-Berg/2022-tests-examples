package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SortPopup extends VertisElement {

    @Name("Сортировка «{{ text }}»")
    @FindBy(".//div[.= '{{ text }}']")
    VertisElement sort(@Param("text") String text);
}