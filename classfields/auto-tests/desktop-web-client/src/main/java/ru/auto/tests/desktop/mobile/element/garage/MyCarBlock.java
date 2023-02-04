package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.page.BasePage;

public interface MyCarBlock extends BasePage, WithButton, WithInput {

    @Name("Кнопка проверить")
    @FindBy(".//button[contains(@class, '_button_size')]")
    VertisElement searchButton();

    @Name("Ошибка")
    @FindBy(".//div[@class = 'VinCheckInput__error']")
    VertisElement errorText();

    @Name("Кнопка «?»")
    @FindBy(".//div[contains(@class, '_questionIcon')]")
    VertisElement questionButton();

}
