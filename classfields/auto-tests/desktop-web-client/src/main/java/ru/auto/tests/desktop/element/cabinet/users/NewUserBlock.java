package ru.auto.tests.desktop.element.cabinet.users;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface NewUserBlock extends VertisElement, WithInput {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//span[contains(@class, 'button__text') and .='{{ text }}'] | " +
            ".//span[contains(@class, 'Button__content') and .='{{ text }}']")
    VertisElement button(@Param("text") String Text);
}
