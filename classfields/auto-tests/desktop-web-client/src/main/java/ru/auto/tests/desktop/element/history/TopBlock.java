package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface TopBlock extends VertisElement, WithButton, WithInput {

    @Name("Кнопка «?»")
    @FindBy(".//div[contains(@class, 'VinCheckInput__questionIcon')]")
    VertisElement questionButton();

    @Name("Ошибка")
    @FindBy(".//div[@class = 'VinCheckSnippetDesktop__error']")
    VinError error();
}
