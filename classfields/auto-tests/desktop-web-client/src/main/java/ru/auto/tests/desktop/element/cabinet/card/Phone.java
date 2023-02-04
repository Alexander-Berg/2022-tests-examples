package ru.auto.tests.desktop.element.cabinet.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface Phone extends VertisElement, WithButton, WithInput {

    @Name("Кнопка удаления телефона")
    @FindBy(".//div[contains(@class, 'CardPhone__clearPhone')]")
    VertisElement deleteButton();
}
