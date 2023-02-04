package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface PromocodeBlock extends VertisElement, WithInput, WithButton {

    @Name("Сообщение об ошибке")
    @FindBy(".//span[contains(@class, 'TextInput__error')]")
    VertisElement errorMessage();

}
