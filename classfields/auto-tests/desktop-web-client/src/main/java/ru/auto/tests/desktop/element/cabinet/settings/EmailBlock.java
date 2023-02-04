package ru.auto.tests.desktop.element.cabinet.settings;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 29.08.18
 */
public interface EmailBlock extends VertisElement {

    @Name("Блок с email")
    @FindBy(".//label")
    VertisElement emailInput();

    @Name("email")
    @FindBy(".//label//input")
    VertisElement currentEmail();

    @Name("Кнопка «Удалить email»")
    @FindBy(".//button[contains(@class, 'SettingsSubscription__remove')]")
    VertisElement remove();

    @Name("Кнопка «Добавить почтовый адрес»")
    @FindBy(".//button[contains(@title, 'Добавить почтовый адрес')]")
    VertisElement add();

    default String getCurrentEmail() {
        return currentEmail().getAttribute("value");
    }
}
