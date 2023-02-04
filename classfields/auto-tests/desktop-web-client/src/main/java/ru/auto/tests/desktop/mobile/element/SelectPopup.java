package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithRadioButton;

public interface SelectPopup extends VertisElement, WithRadioButton {

    @Name("Пункт «{{ text }}»")
    @FindBy(".//div[contains(@class, 'MenuItem') and .='{{ text }}'] |" +
            ".//div[contains(@class, 'menu-item') and .='{{ text }}']")
    VertisElement item(@Param("text") String text);

}
