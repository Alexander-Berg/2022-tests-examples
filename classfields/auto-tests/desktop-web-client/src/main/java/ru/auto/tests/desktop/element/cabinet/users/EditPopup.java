package ru.auto.tests.desktop.element.cabinet.users;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithSelect;

public interface EditPopup extends VertisElement, WithSelect {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//span[contains(@class, 'Button') and .='{{ text }}']")
    VertisElement button(@Param("text") String Text);
}

