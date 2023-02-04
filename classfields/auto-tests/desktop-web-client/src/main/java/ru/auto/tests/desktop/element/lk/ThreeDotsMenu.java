package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ThreeDotsMenu extends VertisElement {

    @Name("Кнопка «{text}» в выпадающем меню")
    @FindBy(".//a[contains(@class,'Link SalesItemNewDesignControls__moreMenuItem') and . = '{{ text }}']")
    VertisElement moreMenuButton(@Param("text") String text);

}