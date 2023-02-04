package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface Sidebar extends VertisElement, WithButton {

    String SIGNIN = "Войти";

    @Name("Кнопка закрытия")
    @FindBy(".//*[contains(@class, 'HeaderNavMenu__close-icon')]")
    VertisElement closeButton();

    @Name("Кнопка выхода")
    @FindBy(".//a[contains(@class, 'HeaderNavMenu__logout')]")
    VertisElement logoutButton();

    @Name("Имя пользователя")
    @FindBy(".//a[contains(@class, 'HeaderNavMenu__itemContent')]")
    VertisElement username();

    @Name("Кнопка ВК")
    @FindBy(".//a[.//*[contains(@class, 'vkontakte')]]")
    VertisElement vkButton();

    @Name("Кнопка Одноклассники")
    @FindBy(".//a[.//*[contains(@class, 'odnoklassniki')]]")
    VertisElement odnoklassnikiButton();

    @Name("Кнопка Youtube")
    @FindBy(".//a[.//*[contains(@class, 'youtube')]]")
    VertisElement youtubeButton();
}
