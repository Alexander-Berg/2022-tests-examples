package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface AuthPopupFrame extends VertisElement, WithButton, WithInput {

    @Name("Заголовок поп-апа авторизации")
    @FindBy(".//div[contains(@class, 'AuthForm__title')]")
    VertisElement title();
}