package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithChat {

    @Name("Чат")
    @FindBy("//div[contains(@class, 'ChatApp_visible')]")
    VertisElement chat();
}
