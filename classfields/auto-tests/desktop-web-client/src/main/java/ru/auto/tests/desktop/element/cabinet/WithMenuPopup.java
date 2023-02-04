package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 09.04.18
 */
public interface WithMenuPopup extends VertisElement {

    @Name("Пункт меню {{ text }}")
    @FindBy(".//div[contains(@class,'MenuItem') and contains(., '{{ text }}')]")
    VertisElement item(@Param("text") String linkText);
}
