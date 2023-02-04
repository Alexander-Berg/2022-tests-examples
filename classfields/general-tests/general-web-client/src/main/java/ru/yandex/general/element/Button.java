package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Button extends VertisElement {

    String ARIA_DISABLED = "aria-disabled";
    String TRUE = "true";
    String FALSE = "false";

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(.,'{{ value }}')]")
    VertisElement button(@Param("value") String value);

    @Name("Кнопка")
    @FindBy(".//button")
    VertisElement button();

}
