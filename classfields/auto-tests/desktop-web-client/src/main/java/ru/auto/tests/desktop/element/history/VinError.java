package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface VinError extends VertisElement {

    @Name("Ошибка")
    @FindBy(".//div[@class = 'VinCheckSnippetDesktop__errorHelp'] | " +
            ".//div[@class = 'VinCheckSnippetMini__errorHelp']")
    VertisElement errorHelp();
}
