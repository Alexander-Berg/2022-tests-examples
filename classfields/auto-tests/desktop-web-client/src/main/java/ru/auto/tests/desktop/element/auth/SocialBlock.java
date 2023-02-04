package ru.auto.tests.desktop.element.auth;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface SocialBlock extends VertisElement, WithButton {

    @Name("Авторизация через Mail.ru")
    @FindBy(".//span[contains(@class, 'Icon_mailru')]")
    VertisElement mailruButton();
}